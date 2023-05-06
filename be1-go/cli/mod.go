// Package main contains the entry point for starting a server.
package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"golang.org/x/exp/slices"
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

	"github.com/fsnotify/fsnotify"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

const (
	// connectionRetryMaxDelay is the maximum time to wait before retrying to connect to a server
	connectionRetryMaxDelay = 32 * time.Second

	// connectionRetryInitialDelay is the initial time to wait before retrying to connect to a server
	connectionRetryInitialDelay = 2 * time.Second

	// connectionRetryRate is the rate at which the time to wait before retrying to connect to a server increases
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

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the server
func Serve(cliCtx *cli.Context) error {
	log := popstellar.Logger

	configFilePath := cliCtx.String("config-file")
	var serverConfig ServerConfig
	var err error

	startedWithConfigFile := false

	// try to start using a config file
	serverConfig, err = startWithConfigFile(configFilePath)
	if err != nil {
		log.Error().Msgf("Could not start with config file: %v", err)
		// try to start using flags instead
		serverConfig, err = startWithFlags(cliCtx)
		if err != nil {
			return xerrors.Errorf("Could not start server: %v", err)
		}
	} else {
		startedWithConfigFile = true
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

	updatedServersChan := make(chan []string)

	// if the config file was used, start watching the other-servers field for changes
	if startedWithConfigFile {
		watcher, err := fsnotify.NewWatcher()
		if err != nil {
			return xerrors.Errorf("Could not create watcher: %v", err)
		}
		defer watcher.Close()

		err = watcher.Add(configFilePath)
		if err != nil {
			return xerrors.Errorf("Could not watch config file: %v", err)
		}

		// start watching goroutine
		go watchConfigFile(watcher, configFilePath, &serverConfig.OtherServers, updatedServersChan)
	}

	connectedServers := make(map[string]bool)

	for _, server := range serverConfig.OtherServers {
		connectedServers[server] = false
	}

	go serverConnectionLoop(h, wg, done, serverConfig.OtherServers, updatedServersChan, &connectedServers)

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
// it also listens for updates in the other-servers field and tries to connect to the new servers
func serverConnectionLoop(h hub.Hub, wg *sync.WaitGroup, done chan struct{}, otherServers []string, updatedServersChan chan []string, connectedServers *map[string]bool) {
	// first connection to the servers
	_ = connectToServers(h, wg, done, otherServers, connectedServers)

	// define the delay between connection retries
	delay := connectionRetryInitialDelay

	ticker := time.NewTicker(delay)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			// try to connect to servers
			log.Info().Msgf("Trying to connect to servers: %v", connectedServers)
			err := connectToServers(h, wg, done, otherServers, connectedServers)
			if err != nil {
				increaseDelay(&delay)
				ticker.Reset(delay)
			} else {
				ticker.Stop()
			}
		case newServersList := <-updatedServersChan:
			log.Info().Msgf("Received servers update")
			delay = connectionRetryInitialDelay
			log.Info().Msgf("Resetting delay to %v", delay)
			ticker.Reset(delay)
			log.Info().Msgf("New servers list: %v", newServersList)
			_ = connectToServers(h, wg, done, newServersList, connectedServers)
		}
	}
}

// increaseDelay increases the delay between connection retries following an exponential backoff
func increaseDelay(delay *time.Duration) {
	if *delay >= connectionRetryMaxDelay {
		*delay = connectionRetryMaxDelay
	} else {
		*delay = *delay * connectionRetryRate
	}
}

// updateServersState updates the state of the servers in the connectedServers map
func updateServersState(servers []string, connectedServers *map[string]bool) {
	for _, server := range servers {
		if _, ok := (*connectedServers)[server]; !ok {
			log.Info().Msgf("Adding server %s to unconnected servers", server)
			(*connectedServers)[server] = false
		}
	}
}

// connectToServers connects to the given remote servers
func connectToServers(h hub.Hub, wg *sync.WaitGroup, done chan struct{}, servers []string, connectedServers *map[string]bool) error {
	log.Info().Msg("Connecting to servers")
	updateServersState(servers, connectedServers)
	var returnErr error
	for serverAddress, connected := range *connectedServers {
		if !connected {
			err := connectToSocket(serverAddress, h, wg, done)
			if err == nil {
				(*connectedServers)[serverAddress] = true
				log.Info().Msgf("connected to server %s", serverAddress)
			} else {
				returnErr = err
				log.Error().Msgf("failed to connect to server %s: %v", serverAddress, err)
			}
		}
	}
	return returnErr
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

// startWithConfigFile returns the ServerConfig from the config file
func startWithConfigFile(configFilename string) (ServerConfig, error) {
	if configFilename == "" {
		return ServerConfig{}, xerrors.Errorf("no config file specified")
	}
	return loadConfig(configFilename)
}

// loadConfig loads the config file
func loadConfig(configFilename string) (ServerConfig, error) {
	bytes, err := os.ReadFile(configFilename)
	if err != nil {
		return ServerConfig{}, xerrors.Errorf("could not read config file: %w", err)
	}
	var config ServerConfig
	err = json.Unmarshal(bytes, &config)
	if err != nil {
		return ServerConfig{}, xerrors.Errorf("could not unmarshal config file: %w", err)
	}
	if config.ServerPort == config.ClientPort {
		return ServerConfig{}, xerrors.Errorf("client and server ports must be different")
	}
	return config, nil
}

// startWithFlags returns the ServerConfig using the command line flags
func startWithFlags(cliCtx *cli.Context) (ServerConfig, error) {
	// get command line args which specify public key, addresses, port to use for clients
	// and servers, remote servers address
	clientPort := cliCtx.Int("client-port")
	serverPort := cliCtx.Int("server-port")
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

// watchConfigFile watches the config file for changes and updates the other servers list if necessary
func watchConfigFile(watcher *fsnotify.Watcher, configFilePath string, otherServers *[]string, updatedServersChan chan []string) {
	log.Info().Msgf("Watching config file %s for changes", configFilePath)
	for event := range watcher.Events {
		if event.Op&fsnotify.Write == fsnotify.Write {
			log.Info().Msgf("Config file %s changed", configFilePath)
			updatedConfig, err := loadConfig(configFilePath)
			if err != nil {
				log.Error().Msgf("Could not load config file: %v", err)
				return
			}
			if newServersAdded(updatedConfig.OtherServers, otherServers) {
				log.Info().Msgf("New servers added to config file: %v", updatedConfig.OtherServers)
				*otherServers = updatedConfig.OtherServers
				log.Info().Msgf("Sending signal to update servers")
				updatedServersChan <- updatedConfig.OtherServers
			}
		}
	}
}

// newServersAdded returns true if there are new servers in the newServers slice
func newServersAdded(newServers []string, oldServers *[]string) bool {
	if len(newServers) != len(*oldServers) {
		return true
	}
	for _, newServer := range newServers {
		if !slices.Contains(*oldServers, newServer) {
			return true
		}
	}
	return false
}
