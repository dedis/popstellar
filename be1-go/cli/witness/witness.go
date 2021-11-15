// Package witness contains the entry point for starting the witness
// server.
package witness

import (
	"encoding/base64"
	be1_go "popstellar"
	"popstellar/channel/lao"
	"popstellar/cli/utility"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the witness.
func Serve(cliCtx *cli.Context) error {
	log := be1_go.Logger

	// get command line args which specify public key, organizer address, port
	// for organizer, clients, witnesses, other witness' addresses
	organizerAddress := cliCtx.String("organizer-address")
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	if clientPort == witnessPort {
		return xerrors.Errorf("client and witness ports must be different")
	}
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

	// create witness hub
	h, err := standard_hub.NewHub(point, log, lao.NewChannel, hub.WitnessHubType)
	if err != nil {
		return xerrors.Errorf("failed create the witness hub: %v", err)
	}

	// launch witness hub
	h.Start()

	// create wait group which waits for goroutines to finish
	wg := &sync.WaitGroup{}
	done := make(chan struct{})

	// connect to organizer's witness endpoint
	err = utility.ConnectToSocket(hub.OrganizerHubType, organizerAddress, h, wg, done)
	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	// connect to other witnesses
	for _, witness := range otherWitness {
		err = utility.ConnectToSocket(hub.WitnessHubType, witness, h, wg, done)
		if err != nil {
			return xerrors.Errorf("failed to connect to witness: %v", err)
		}
	}

	// create and serve servers for witnesses and clients
	clientSrv := network.NewServer(h, clientPort, socket.ClientSocketType, log)
	clientSrv.Start()

	witnessSrv := network.NewServer(h, witnessPort, socket.WitnessSocketType, log)
	witnessSrv.Start()

	// shut down client server and witness server when ctrl+c received
	err = network.WaitAndShutdownServers(clientSrv, witnessSrv)
	if err != nil {
		return err
	}
	<-witnessSrv.Stopped
	<-clientSrv.Stopped

	// stop the hub
	h.Stop()
	close(done)
	wg.Wait()

	return nil
}
