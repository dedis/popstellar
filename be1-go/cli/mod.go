// Package cli contains the entry point for starting the organizer
// server.
package main

import (
	"encoding/base64"
	"fmt"
	"github.com/gorilla/websocket"
	"net/url"
	be1_go "popstellar"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"
	"time"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

const witness string = "witness"
const organizer string = "organizer"

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the organizer or the witness
func Serve(cliCtx *cli.Context, user string) error {
	log := be1_go.Logger

	if user != organizer && user != witness {
		return xerrors.Errorf("unrecognized user, should be \"organizer\" or \"witness\"")
	}

	// get command line args which specify public key, port to use for clients
	// and witnesses, witness' address
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	if clientPort == witnessPort {
		return xerrors.Errorf("client and witness ports must be different")
	}
	otherWitness := cliCtx.StringSlice("other-witness")

	pk := cliCtx.String("public-key")
	if pk == "" {
		return xerrors.Errorf("%s's public key is required", user)
	}

	// decode public key and unmarshal public key
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		return xerrors.Errorf("failed to base64url decode public key: %v", err)
	}

	point := crypto.Suite.Point()

	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal public key: %v", err)
	}

	// get the HubType from the user
	var hubType = hub.HubType(user)

	// create user hub
	h, err := standard_hub.NewHub(point, log.With().Str("role", user).Logger(), lao.NewChannel, hubType)
	if err != nil {
		return xerrors.Errorf("failed create the %s hub: %v", user, err)
	}

	// start the processing loop
	h.Start()

	// Start a client websocket server
	clientSrv := network.NewServer(h, clientPort, socket.ClientSocketType,
		log.With().Str("role", "client server").Logger())
	clientSrv.Start()

	// Start a witness websocket server
	witnessSrv := network.NewServer(h, witnessPort, socket.WitnessSocketType,
		log.With().Str("role", "witness server").Logger())
	witnessSrv.Start()

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}
	done := make(chan struct{})

	if user == "witness" {
		organizerAddress := cliCtx.String("organizer-address")

		// connect to organizer's witness endpoint
		err = connectToSocket(hub.OrganizerHubType, organizerAddress, h, wg, done)
		if err != nil {
			return xerrors.Errorf("failed to connect to organizer: %v", err)
		}
	}

	// connect to given witness
	for _, witnessAddress := range otherWitness {
		err = connectToSocket(hub.WitnessHubType, witnessAddress, h, wg, done)
		if err != nil {
			return xerrors.Errorf("failed to connect to witness: %v", err)
		}
	}

	// Wait for a Ctrl-C
	err = network.WaitAndShutdownServers(clientSrv, witnessSrv)
	if err != nil {
		return err
	}

	h.Stop()
	<-clientSrv.Stopped
	<-witnessSrv.Stopped

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

// connectToSocket establishes a connection to another server's witness
// endpoint.
func connectToSocket(otherHubType hub.HubType, address string, h hub.Hub, wg *sync.WaitGroup, done chan struct{}) error {
	log := be1_go.Logger

	urlString := fmt.Sprintf("ws://%s/%s/witness", address, otherHubType)
	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s: %v", urlString, err)
	}

	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial to %s: %v", u.String(), err)
	}

	log.Info().Msgf("connected to %s at %s", otherHubType, urlString)

	switch otherHubType {
	case hub.OrganizerHubType:
		organizerSocket := socket.NewOrganizerSocket(h.Receiver(),
			h.OnSocketClose(), ws, wg, done, log)
		wg.Add(2)

		go organizerSocket.WritePump()
		go organizerSocket.ReadPump()

		err = h.NotifyNewServer(organizerSocket)
		if err != nil {
			return xerrors.Errorf("error while trying to catchup to server: %v", err)
		}
	case hub.WitnessHubType:
		witnessSocket := socket.NewWitnessSocket(h.Receiver(),
			h.OnSocketClose(), ws, wg, done, log)
		wg.Add(2)

		go witnessSocket.WritePump()
		go witnessSocket.ReadPump()

		err = h.NotifyNewServer(witnessSocket)
		if err != nil {
			return xerrors.Errorf("error while trying to catchup to server: %v", err)
		}
	default:
		return xerrors.Errorf("invalid other hub type: %v", otherHubType)
	}

	return nil
}
