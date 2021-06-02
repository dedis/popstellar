package witness

import (
	"context"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"student20_pop"
	"student20_pop/hub"
	"sync"
	"syscall"

	"github.com/gorilla/websocket"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func Serve(cliCtx *cli.Context) error {
	organizerAddress := cliCtx.String("organizer-address")
	organizerPort := cliCtx.Int("organizer-port")
	clientPort := cliCtx.Int("client-port")
	witnessPort := cliCtx.Int("witness-port")
	otherWitness := cliCtx.StringSlice("other-witness")
	pk := cliCtx.String("public-key")

	if pk == "" {
		return xerrors.Errorf("witness' public key is required")
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

	h := hub.NewWitnessHub(point)

	ctx, cancel := context.WithCancel(cliCtx.Context)
	defer cancel()

	wg := &sync.WaitGroup{}

	err = connectToSocket(ctx, hub.OrganizerSocketType, organizerAddress, h, organizerPort, wg)
	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	for _, otherWit := range otherWitness {
		err = connectToSocket(ctx, hub.WitnessSocketType, otherWit, h, organizerPort, wg)
		if err != nil {
			return xerrors.Errorf("failed to connect to witness: %v", err)
		}
	}

	clientSrv := hub.CreateAndServeWS(ctx, hub.WitnessHubType, hub.ClientSocketType, h, clientPort, wg)
	witnessSrv := hub.CreateAndServeWS(ctx, hub.WitnessHubType, hub.WitnessSocketType, h, witnessPort, wg)

	go func() {
		defer cancel()
		done := make(chan os.Signal, 1)
		signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)
		<-done

		err := clientSrv.Shutdown(ctx)
		if err != nil {
			log.Fatalf("failed to shutdown client server: %v", err)
		}

		err = witnessSrv.Shutdown(ctx)
		if err != nil {
			log.Fatalf("failed to shutdown witness server: %v", err)
		}

	}()

	go h.Start(ctx, wg)

	wg.Wait()

	return nil
}

func connectToSocket(ctx context.Context, socketType hub.SocketType, address string, h hub.Hub, port int, wg *sync.WaitGroup) error {
	var urlString string
	switch socketType {
	case hub.OrganizerSocketType:
		urlString = fmt.Sprintf("ws://%s:%d/%s/witness/", address, port, socketType)
	case hub.WitnessSocketType:
		if address == "" {
			return nil
		}
		urlString = fmt.Sprintf("ws://%s/%s/witness/", address, socketType)
	}

	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s %v", urlString, err)
	}

	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial %v", err)
	}

	log.Printf("connected to %s at %s", socketType, urlString)

	switch socketType {
	case hub.OrganizerSocketType:
		organizerSocket := hub.NewOrganizerSocket(h, ws, wg)
		go organizerSocket.WritePump(ctx)
		go organizerSocket.ReadPump(ctx)
	case hub.WitnessSocketType:
		witnessSocket := hub.NewWitnessSocket(h, ws, wg)
		go witnessSocket.WritePump(ctx)
		go witnessSocket.ReadPump(ctx)
	}

	return nil
}
