// Package main contains the entry point for starting a server.
package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/fsnotify/fsnotify"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"github.com/urfave/cli/v2"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/exp/slices"
	"golang.org/x/xerrors"
	"net/url"
	"os"
	"popstellar/internal/crypto"
	"popstellar/internal/hub"
	"popstellar/internal/logger"
	"popstellar/internal/network"
	"popstellar/internal/network/socket"
	oldHub "popstellar/internal/old/hub"
	"sync"
	"time"
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
	ClientAddress  string   `json:"client-address"`
	ServerAddress  string   `json:"server-address"`
	PublicAddress  string   `json:"server-public-address"`
	PrivateAddress string   `json:"server-listen-address"`
	AuthAddress    string   `json:"auth-server-address"`
	ClientPort     int      `json:"client-port"`
	ServerPort     int      `json:"server-port"`
	AuthPort       int      `json:"auth-port"`
	OtherServers   []string `json:"other-servers"`
	DatabasePath   string   `json:"database-path"`
}

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the server
func Serve(cliCtx *cli.Context) error {
	log := logger.Logger

	configFilePath := cliCtx.String("config-file")
	var serverConfig ServerConfig
	var err error

	startedWithConfigFile := false

	// start using a config file if a file path was provided
	// otherwise start using the flags
	if configFilePath != "" {
		serverConfig, err = startWithConfigFile(configFilePath)
		if err != nil {
			return xerrors.Errorf("Could not start server with config file: %v", err)
		}
		startedWithConfigFile = true
	} else {
		serverConfig, err = startWithFlags(cliCtx)
		if err != nil {
			return xerrors.Errorf("Could not start server using flags: %v", err)
		}
	}

	h, err := serverConfig.newHub(log)
	if err != nil {
		return err
	}

	// start the processing loop
	h.Start()

	// Start websocket server for clients
	clientSrv := network.NewServer(h, serverConfig.PrivateAddress, serverConfig.ClientPort, socket.ClientSocketType, log)
	clientSrv.Start()

	// Start a websocket server for remote servers
	serverSrv := network.NewServer(h, serverConfig.PrivateAddress, serverConfig.ServerPort, socket.ServerSocketType, log)
	serverSrv.Start()

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}
	done := make(chan struct{})

	// create channel for updated servers from the config file
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
		go watchConfigFile(watcher, configFilePath, &serverConfig.OtherServers, updatedServersChan, log)
	}

	// map to keep track of the connection status of the servers
	connectedServers := make(map[string]bool)

	for _, server := range serverConfig.OtherServers {
		connectedServers[server] = false
	}

	wg.Add(1)
	go serverConnectionLoop(h, wg, done, serverConfig.OtherServers, updatedServersChan, &connectedServers, log)

	// Wait for a Ctrl-C
	err = network.WaitAndShutdownServers(cliCtx.Context, nil, clientSrv, serverSrv)
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

func (s *ServerConfig) newHub(log zerolog.Logger) (oldHub.Hub, error) {
	// compute the client server address if it wasn't provided
	if s.ClientAddress == "" {
		s.ClientAddress = fmt.Sprintf("ws://%s:%d/client", s.PublicAddress, s.ClientPort)
	}
	// compute the server server address if it wasn't provided
	if s.ServerAddress == "" {
		s.ServerAddress = fmt.Sprintf("ws://%s:%d/server", s.PublicAddress, s.ServerPort)
	}

	var ownerPubKey kyber.Point = nil
	err := ownerKey(s.PublicKey, &ownerPubKey, log)
	if err != nil {
		return nil, err
	}

	return hub.New(s.DatabasePath, ownerPubKey, s.ClientAddress, s.ServerAddress, log)
}

// serverConnectionLoop tries to connect to the remote servers following an exponential backoff strategy
// it also listens for updates in the other-servers field and tries to connect to the new servers
func serverConnectionLoop(h oldHub.Hub, wg *sync.WaitGroup, done chan struct{}, otherServers []string,
	updatedServersChan chan []string, connectedServers *map[string]bool, log zerolog.Logger) {
	// first connection to the servers
	serversToConnect := otherServers
	_ = connectToServers(h, wg, done, serversToConnect, connectedServers, log)

	// define the delay between connection retries
	delay := connectionRetryInitialDelay

	ticker := time.NewTicker(delay)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			// try to connect to servers
			log.Info().Msgf("Trying to connect to servers: %v", serversToConnect)
			err := connectToServers(h, wg, done, serversToConnect, connectedServers, log)
			if err != nil {
				increaseDelay(&delay)
				ticker.Reset(delay)
			} else {
				ticker.Stop()
			}
		case newServersList := <-updatedServersChan:
			delay = connectionRetryInitialDelay
			ticker.Reset(delay)
			serversToConnect = newServersList
			_ = connectToServers(h, wg, done, serversToConnect, connectedServers, log)
		case <-done:
			log.Info().Msg("Stopping the server connection loop")
			wg.Done()
			return
		}
	}
}

// connectToServers updates the connection status of the servers and tries to connect to the ones that are not connected
// it returns an error if at least one connection fails
func connectToServers(h oldHub.Hub, wg *sync.WaitGroup, done chan struct{}, servers []string,
	connectedServers *map[string]bool, log zerolog.Logger) error {
	updateServersState(servers, connectedServers)
	var returnErr error
	for serverAddress, connected := range *connectedServers {
		if !connected {
			err := connectToSocket(serverAddress, h, wg, done, log)
			if err == nil {
				(*connectedServers)[serverAddress] = true
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
func connectToSocket(address string, h oldHub.Hub, wg *sync.WaitGroup, done chan struct{}, log zerolog.Logger) error {
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

	err = h.SendGreetServer(remoteSocket)
	if err != nil {
		return xerrors.Errorf("failed to send greet to server: %v", err)
	}

	h.NotifyNewServer(remoteSocket)

	return nil
}

func ownerKey(pk string, point *kyber.Point, log zerolog.Logger) error {
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
		return ServerConfig{}, xerrors.Errorf("could not read serverConfig file: %w", err)
	}
	var serverConfig ServerConfig
	err = json.Unmarshal(bytes, &serverConfig)
	if err != nil {
		return ServerConfig{}, xerrors.Errorf("could not unmarshal serverConfig file: %w", err)
	}
	if serverConfig.ServerPort == serverConfig.ClientPort {
		return ServerConfig{}, xerrors.Errorf("client and server ports must be different")

	} else if serverConfig.ServerPort == serverConfig.AuthPort || serverConfig.ClientPort == serverConfig.AuthPort {
		return ServerConfig{}, xerrors.Errorf("PoPCHA Authentication port must be unique\"")
	}
	return serverConfig, nil
}

// startWithFlags returns the ServerConfig using the command line flags
func startWithFlags(cliCtx *cli.Context) (ServerConfig, error) {
	// get command line args which specify public key, addresses, port to use for clients
	// and servers, remote servers address
	clientPort := cliCtx.Int("client-port")
	serverPort := cliCtx.Int("server-port")
	authPort := cliCtx.Int("auth-port")
	if clientPort == serverPort {
		return ServerConfig{}, xerrors.Errorf("client and server ports must be different")
	} else if clientPort == authPort || serverPort == authPort {
		return ServerConfig{}, xerrors.Errorf("PoPCHA Authentication port must be unique")
	}
	return ServerConfig{
		PublicKey:      cliCtx.String("public-key"),
		ClientAddress:  cliCtx.String("client-address"),
		ServerAddress:  cliCtx.String("server-address"),
		PublicAddress:  cliCtx.String("server-public-address"),
		PrivateAddress: cliCtx.String("server-listen-address"),
		AuthAddress:    cliCtx.String("auth-server-address"),
		ClientPort:     clientPort,
		ServerPort:     serverPort,
		AuthPort:       authPort,
		OtherServers:   cliCtx.StringSlice("other-servers"),
		DatabasePath:   cliCtx.String("database-path"),
	}, nil
}

// watchConfigFile watches the config file for changes, updates the other servers list in the config if necessary
// and sends the updated other servers list to the updatedServersChan so that the connection to servers loop can
// connect to them and update their connection status
func watchConfigFile(watcher *fsnotify.Watcher, configFilePath string, otherServersField *[]string,
	updatedServersChan chan []string, log zerolog.Logger) {
	for event := range watcher.Events {
		if event.Op&fsnotify.Write == fsnotify.Write {
			updatedConfig, err := loadConfig(configFilePath)
			if err != nil {
				log.Error().Msgf("Could not load config file: %v", err)
			} else if newServersAdded(updatedConfig.OtherServers, otherServersField) {
				log.Info().Msgf("New servers list: %v", updatedConfig.OtherServers)
				// update the other servers field of the config
				*otherServersField = updatedConfig.OtherServers
				// send the updated other servers list to the channel
				updatedServersChan <- updatedConfig.OtherServers
			}
		}
	}
}

// newServersAdded returns true if the new servers and old servers slices are different
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

// increaseDelay increases the delay between connection retries following an exponential backoff
func increaseDelay(delay *time.Duration) {
	if *delay >= connectionRetryMaxDelay {
		*delay = connectionRetryMaxDelay
	} else {
		*delay = *delay * connectionRetryRate
	}
}

// updateServersState adds servers to the connected servers map if they are not already in it
func updateServersState(servers []string, connectedServers *map[string]bool) {
	for _, server := range servers {
		if _, ok := (*connectedServers)[server]; !ok {
			(*connectedServers)[server] = false
		}
	}
}
