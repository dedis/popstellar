// Package main contains the entry point for starting the organizer
// or witness server.
package main

import (
	"encoding/base64"
	"fmt"
	"net/url"
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

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the organizer or the witness
func Serve(cliCtx *cli.Context) error {
	log := popstellar.Logger

	// get command line args which specify public key, addresses, port to use for clients
	// and witnesses, witness' address
	publicAddress := cliCtx.String("server-public-address")
	privateAddress := cliCtx.String("server-listen-address")

	clientPort := cliCtx.Int("client-port")
	serverPort := cliCtx.Int("server-port")
	if clientPort == serverPort {
		return xerrors.Errorf("client and server ports must be different")
	}
	otherServers := cliCtx.StringSlice("other-servers")

	pk := cliCtx.String("public-key")

	// compute the client server address
	clientServerAddress := fmt.Sprintf("%s:%d", publicAddress, clientPort)

	var point kyber.Point = nil
	ownerKey(pk, &point)

	// create user hub
	h, err := standard_hub.NewHub(point, clientServerAddress, log.With().Str("role", "server").Logger(),
		lao.NewChannel)
	if err != nil {
		return xerrors.Errorf("failed create the hub: %v", err)
	}

	// start the processing loop
	h.Start()

	// Start a client websocket server
	clientSrv := network.NewServer(h, privateAddress, clientPort, socket.ClientSocketType,
		log.With().Str("role", "client server").Logger())
	clientSrv.Start()

	// Start a witness websocket server
	serverSrv := network.NewServer(h, privateAddress, serverPort, socket.ServerSocketType,
		log.With().Str("role", "server server").Logger())
	serverSrv.Start()

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}
	done := make(chan struct{})

	// connect to given witness
	for _, serverAddress := range otherServers {
		err = connectToSocket(serverAddress, h, wg, done)
		if err != nil {
			return xerrors.Errorf("failed to connect to server: %v", err)
		}
	}

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

// connectToSocket establishes a connection to another server's server
// endpoint.
func connectToSocket(address string, h hub.Hub,
	wg *sync.WaitGroup, done chan struct{}) error {

	log := popstellar.Logger

	urlString := fmt.Sprintf("ws://%s/server/server", address)
	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s: %v", urlString, err)
	}
	log.Info().Msgf("test")
	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial to %s: %v", u.String(), err)
	}

	log.Info().Msgf("connected to server at %s", urlString)

	organizerSocket := socket.NewServerSocket(h.Receiver(),
		h.OnSocketClose(), ws, wg, done, log)
	wg.Add(2)

	go organizerSocket.WritePump()
	go organizerSocket.ReadPump()

	err = h.NotifyNewServer(organizerSocket)
	if err != nil {
		return xerrors.Errorf("error while trying to catchup to server: %v", err)
	}

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
