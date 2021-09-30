// Package organizer contains the entry point for starting the organizer
// server.
package organizer

import (
	"encoding/base64"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
	be1_go "popstellar"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub/organizer"
	"popstellar/network"
	"popstellar/network/socket"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server for
// the organizer.
func Serve(cliCtx *cli.Context) error {
	log := be1_go.Logger

	// get command line args which specify public key, port to use for clients
	// and witnesses
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

	// create organizer hub
	h, err := organizer.NewHub(point, log.With().Str("role", "organizer").Logger(), lao.NewChannel)
	if err != nil {
		return xerrors.Errorf("failed create the organizer hub: %v", err)
	}

	// Start a client websocket server
	clientSrv := network.NewServer(h, clientPort, socket.ClientSocketType,
		log.With().Str("role", "client server").Logger())
	clientSrv.Start()

	// Start a witness websocket server
	witnessSrv := network.NewServer(h, witnessPort, socket.WitnessSocketType,
		log.With().Str("role", "witness server").Logger())
	witnessSrv.Start()

	// start the processing loop
	h.Start()

	// Wait for a Ctrl-C
	err = network.WaitAndShutdownServers(clientSrv, witnessSrv)
	if err != nil {
		return err
	}

	h.Stop()
	<-clientSrv.Stopped
	<-witnessSrv.Stopped

	return nil
}
