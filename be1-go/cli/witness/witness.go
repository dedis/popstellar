// Package witness contains the entry point for starting the witness
// server.
package witness

import (
	"context"
	"encoding/base64"
	"fmt"
	"log"
	"net/url"
	"student20_pop/crypto"
	"student20_pop/hub"
	"student20_pop/network"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server
// for the witness.
func Serve(cliCtx *cli.Context) error {

	// get command line args which specify public key, organizer address, port for organizer,
	// clients, witnesses, other witness' addresses
	organizerAddress := cliCtx.String("organizer-address")
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	otherWitness := cliCtx.StringSlice("other-witness")
	pk := cliCtx.String("public-key")
	if pk == "" {
		return xerrors.Errorf("witness' public key is required")
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

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}

	// create witness hub
	h, err := hub.NewWitnessHub(point, wg)
	if err != nil {
		return xerrors.Errorf("failed create the witness hub: %v", err)
	}
	// make context release resources associated with it when all operations are done
	ctx, cancel := context.WithCancel(cliCtx.Context)
	defer cancel()

	// increment wait group and connect to organizer's witness endpoint
	err = connectToWitnessSocket(ctx, hub.OrganizerHubType, organizerAddress, h, wg)
	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	// increment wait group and connect to other witnesses
	for _, witness := range otherWitness {
		err = connectToWitnessSocket(ctx, hub.WitnessHubType, witness, h, wg)
		if err != nil {
			return xerrors.Errorf("failed to connect to witness: %v", err)
		}
	}

	// increment wait group and create and serve servers for witnesses and clients
	clientSrv := network.NewServer(ctx, h, clientPort, hub.ClientSocketType, wg)
	clientSrv.Start()

	witnessSrv := network.NewServer(ctx, h, witnessPort, hub.WitnessSocketType, wg)
	witnessSrv.Start()

	// increment wait group and launch organizer hub
	go h.Start(ctx)

	// shut down client server and witness server when ctrl+c received
	network.WaitAndShutdownServers(clientSrv, witnessSrv)

	// cancel the context
	cancel()

	// wait for all goroutines to finish
	wg.Wait()

	return nil
}

// connectToSocket establishes a connection to another server's witness
// endpoint.
func connectToWitnessSocket(ctx context.Context, otherHubType hub.HubType, address string, h hub.Hub, wg *sync.WaitGroup) error {
	urlString := fmt.Sprintf("ws://%s/%s/witness/", address, otherHubType)
	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s %v", urlString, err)
	}

	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial: %v", err)
	}

	log.Printf("connected to %s at %s", otherHubType, urlString)

	switch otherHubType {
	case hub.OrganizerHubType:
		organizerSocket := hub.NewOrganizerSocket(h.Receiver(), h.OnSocketClose(), ws, wg)
		go organizerSocket.WritePump(ctx)
		go organizerSocket.ReadPump(ctx)
	case hub.WitnessHubType:
		witnessSocket := hub.NewWitnessSocket(h.Receiver(), h.OnSocketClose(), ws, wg)
		go witnessSocket.WritePump(ctx)
		go witnessSocket.ReadPump(ctx)
	}

	return nil
}
