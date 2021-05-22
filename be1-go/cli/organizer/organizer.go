package organizer

import (
	"encoding/base64"
	"student20_pop"
	"student20_pop/hub"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server.
func Serve(context *cli.Context) error {
	clientPort := context.Int("client-port")
	witnessPort := context.Int("witness-port")
	if clientPort == witnessPort {
		return xerrors.Errorf("client and witness ports must be different")
	}

	pk := context.String("public-key")

	if pk == "" {
		return xerrors.Errorf("organizer's public key is required")
	}

	pkBuf, err := base64.StdEncoding.DecodeString(pk)
	if err != nil {
		return xerrors.Errorf("failed to base64 decode public key: %v", err)
	}

	point := student20_pop.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal public key: %v", err)
	}

	h, err := hub.NewOrganizerHub(point)
	if err != nil {
		return xerrors.Errorf("failed create the organizer hub: %v", err)
	}

	go hub.CreateAndServeWs(hub.OrganizerHubType, hub.WitnessSocketType, h, witnessPort)
	go hub.CreateAndServeWs(hub.OrganizerHubType, hub.ClientSocketType, h, clientPort)

	done := make(chan struct{})
	h.Start(done)

	done <- struct{}{}

	return nil
}
