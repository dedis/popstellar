package organizer

import (
	"context"
	"encoding/base64"
	"log"
	"os"
	"os/signal"
	"student20_pop"
	"student20_pop/hub"
	"sync"
	"syscall"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

// Serve parses the CLI arguments and spawns a hub and a websocket server.
func Serve(cliCtx *cli.Context) error {
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	if clientPort == witnessPort {
		return xerrors.Errorf("client and witness ports must be different")
	}

	pk := cliCtx.String("public-key")

	if pk == "" {
		return xerrors.Errorf("organizer's public key is required")
	}

	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		return xerrors.Errorf("failed to base64url decode public key: %v", err)
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

	ctx, cancel := context.WithCancel(cliCtx)
	defer cancel()

	wg := &sync.WaitGroup{}

	witnessSrv := hub.CreateAndServeWS(ctx, hub.OrganizerHubType, hub.WitnessSocketType, h, witnessPort, wg)
	clientSrv := hub.CreateAndServeWS(ctx, hub.OrganizerHubType, hub.ClientSocketType, h, clientPort, wg)

	go func() {
		defer cancel()
		done := make(chan os.Signal, 1)
		signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)
		<-done

		log.Println("received ctrl+c")
		err := clientSrv.Shutdown(ctx)
		if err != nil {
			log.Fatalf("failed to shutdown client server: %v", err)
		}

		err = witnessSrv.Shutdown(ctx)
		if err != nil {
			log.Fatalf("failed to shutdown witness server: %v", err)
		}

		log.Println("shutdown both servers")
	}()

	go h.Start(ctx, wg)

	wg.Wait()

	return nil
}
