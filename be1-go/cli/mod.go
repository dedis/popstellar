// Package main contains the entry point for starting a server.
package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/url"
	"os"
	popstellar "popstellar"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"
	"time"

	"github.com/rs/zerolog/log"
	"go.dedis.ch/kyber/v3"

	"github.com/gorilla/websocket"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

const (
	// connectionRetryMaxDelay is the maximum time to wait before retrying to connect to a server
	connectionRetryMaxDelay = 32 * time.Second

	connectionRetryInitialDelay = 2 * time.Second

	connectionRetryRate = 2
)

// ServerConfig contains the configuration for the server
type ServerConfig struct {
	PublicKey      string   `json:"public-key"`
	PublicAddress  string   `json:"server-public-address"`
	PrivateAddress string   `json:"server-listen-address"`
	ClientPort     int      `json:"client-port"`
	ServerPort     int      `json:"server-port"`
	OtherServers   []string `json:"other-servers"`
}

func startWithConfigFile(configFilename string) (ServerConfig, error) {
	if configFilename == "" {
		return ServerConfig{}, xerrors.Errorf("no config file specified")
	}
	bytes, err := os.ReadFile(configFilename)
	if err != nil {
		return ServerConfig{}, xerrors.Errorf("could not read config file: %w", err)
	}
	var config ServerConfig
	err = json.Unmarshal(bytes, &config)
	if err != nil {
		return ServerConfig{}, xerrors.Errorf("could not unmarshal config file: %w", err)
	}
	return config, nil
}

func startWithFlags(cliCtx *cli.Context) (ServerConfig, error) {
	// get command line args which specify public key, addresses, port to use for clients
	// and servers, remote servers address
	clientPort := cliCtx.Int("client-port")
	serverPort := cliCtx.Int("server-port")
	log.Info().Msgf("Starting server with client port %d and server port %d", clientPort, serverPort)
	if clientPort == serverPort {
		return ServerConfig{}, xerrors.Errorf("client and server ports must be different")
	}
	return ServerConfig{
		PublicKey:      cliCtx.String("public-key"),
		PublicAddress:  cliCtx.String("server-public-address"),
		PrivateAddress: cliCtx.String("server-listen-address"),
		ClientPort:     clientPort,
		ServerPort:     serverPort,
		OtherServers:   cliCtx.StringSlice("other-servers"),
	}, nil
}

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the server
func Serve(cliCtx *cli.Context) error {
	log := popstellar.Logger

	configFilePath := cliCtx.String("config-file")
	var serverConfig ServerConfig
	var err error

	serverConfig, err = startWithConfigFile(configFilePath)
	if err != nil {
		log.Error().Msgf("Could not start with config file: %v", err)
		serverConfig, err = startWithFlags(cliCtx)
	}

	// compute the client server address
	clientServerAddress := fmt.Sprintf("%s:%d", serverConfig.PublicAddress, serverConfig.ClientPort)

	var point kyber.Point = nil
	ownerKey(serverConfig.PublicKey, &point)

	// create user hub
	h, err := standard_hub.NewHub(point, clientServerAddress, log.With().Str("role", "server").Logger(),
		lao.NewChannel)
	if err != nil {
		return xerrors.Errorf("failed create the hub: %v", err)
	}

	// start the processing loop
	h.Start()

	// Start websocket server for clients
	clientSrv := network.NewServer(h, serverConfig.PrivateAddress, serverConfig.ClientPort, socket.ClientSocketType,
		log.With().Str("role", "client websocket").Logger())
	clientSrv.Start()

	// Start a websocket server for remote servers
	serverSrv := network.NewServer(h, serverConfig.PrivateAddress, serverConfig.ServerPort, socket.ServerSocketType,
		log.With().Str("role", "server websocket").Logger())
	serverSrv.Start()

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}
	done := make(chan struct{})

	// create the map of servers and their connection status
	connectedServers := make(map[string]bool)
	for _, serverAddress := range serverConfig.OtherServers {
		connectedServers[serverAddress] = false
	}

	// start the connection to servers loop
	go serverConnectionLoop(h, wg, done, connectedServers)

	// Wait for a Ctrl-C
	err = network.WaitAndShutdownServers(cliCtx.Context, clientSrv, serverSrv)
	if err != nil {
		return err
	}

	h.Stop()
	<-clientSrv.Stopped
	<-serverSrv.Stopped

	// notify channs to stop
	close(done)

	// wait on channs to be done
	channsClosed := make(chan struct{})
	go func() {
		wg.Wait()
		close(channsClosed)
	}()

	select {
	case <-channsClosed:
	case <-time.After(time.Second * 10):
		log.Error().Msg("channs didn't close after timeout, exiting")
	}

	return nil
}

// serverConnectionLoop tries to connect to the remote servers following an exponential backoff strategy
func serverConnectionLoop(h hub.Hub, wg *sync.WaitGroup, done chan struct{}, connectedServers map[string]bool) {
	// first connection to the servers
	_ = connectToServers(h, wg, done, connectedServers)

	// define the delay between connection retries
	delay := connectionRetryInitialDelay

	ticker := time.NewTicker(delay)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			// try to connect to servers
			err := connectToServers(h, wg, done, connectedServers)
			if err != nil {
				increaseDelay(&delay)
				log.Info().Msgf("Increasing delay to %v", delay)
			}
		}
	}
}

// increaseDelay increases the delay between connection retries following an exponential backoff
func increaseDelay(delay *time.Duration) {
	if *delay > connectionRetryMaxDelay {
		*delay = connectionRetryMaxDelay
	} else {
		*delay = *delay * connectionRetryRate
	}
}

// connectToServers connects to the given remote servers
func connectToServers(h hub.Hub, wg *sync.WaitGroup, done chan struct{}, connectedServers map[string]bool) error {
	log.Info().Msg("Connecting to servers")
	for serverAddress, connected := range connectedServers {
		if !connected {
			err := connectToSocket(serverAddress, h, wg, done)
			if err == nil {
				connectedServers[serverAddress] = true
				log.Info().Msgf("connected to server %s", serverAddress)
			} else {
				log.Error().Msgf("failed to connect to server %s: %v", serverAddress, err)
			}
		}
	}
	return nil
}

// connectToSocket establishes a connection to another server's server
// endpoint.
func connectToSocket(address string, h hub.Hub,
	wg *sync.WaitGroup, done chan struct{}) error {

	log := popstellar.Logger

	urlString := fmt.Sprintf("ws://%s/server", address)
	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s: %v", urlString, err)
	}

	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial to %s: %v", u.String(), err)
	}

	log.Info().Msgf("connected to server at %s", urlString)

	remoteSocket := socket.NewServerSocket(h.Receiver(),
		h.OnSocketClose(), ws, wg, done, log)
	wg.Add(2)

	go remoteSocket.WritePump()
	go remoteSocket.ReadPump()

	h.NotifyNewServer(remoteSocket)

	return nil
}

func ownerKey(pk string, point *kyber.Point) error {
	if pk != "" {
		*point = crypto.Suite.Point()
		// decode public key and unmarshal public key
		pkBuf, err := base64.URLEncoding.DecodeString(pk)
		if err != nil {
			return xerrors.Errorf("failed to base64url decode public key: %v", err)
		}

		err = (*point).UnmarshalBinary(pkBuf)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal public key: %v", err)
		}

		log.Info().Msg("The owner public key has been specified, only " + pk + " can create LAO")
	} else {
		log.Info().Msg("No public key specified for the owner, everyone can create LAO.")
	}

	return nil
}
