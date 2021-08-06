// Package organizer contains the entry point for starting the organizer
// server.
package organizer

import (
	"context"
	"encoding/base64"
	"student20_pop/crypto"
	"student20_pop/hub"
	"student20_pop/network"
	"student20_pop/network/socket"
	"sync"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server
// for the organizer.
func Serve(cliCtx *cli.Context) error {

	// get command line args which specify public key, port to use for clients and witnesses
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	if clientPort == witnessPort {
		return xerrors.Errorf("client and witness ports must be different")
	}

	pk := cliCtx.String("public-key")
	if pk == "" {
		return xerrors.Errorf("organizer's public key is required")
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

	// create organizer hub
	h, err := hub.NewOrganizerHub(point, wg)
	if err != nil {
		return xerrors.Errorf("failed create the organizer hub: %v", err)
	}

	// make context release resources associated with it when all operations are done
	ctx, cancel := context.WithCancel(cliCtx.Context)
	defer cancel()

	// increment wait group and create and serve servers for witnesses and clients
	clientSrv := network.NewServer(ctx, h, clientPort, socket.ClientSocketType, wg)
	clientSrv.Start()

	witnessSrv := network.NewServer(ctx, h, witnessPort, socket.WitnessSocketType, wg)
	witnessSrv.Start()

	// start the processing loop
	go h.Start(ctx)

	network.WaitAndShutdownServers(clientSrv, witnessSrv)

	// cancel the context
	cancel()

	// wait for all goroutines to finish
	wg.Wait()

	return nil
}
