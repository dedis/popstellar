package witness

import (
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"student20_pop"
	"student20_pop/hub"

	"github.com/gorilla/websocket"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func Serve(context *cli.Context) error {
	organizerAddress := context.String("organizer-address")
	organizerPort := context.Int("organizer-port")
	clientPort := context.Int("client-port")
	witnessPort := context.Int("witness-port")
	otherWitness := context.StringSlice("other-witness")
	pk := context.String("public-key")

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

	err = connectToSocket(hub.OrganizerSocketType, organizerAddress, h, organizerPort)
	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	for _, otherWit := range otherWitness {
		err = connectToSocket(hub.WitnessSocketType, otherWit, h, organizerPort)
		if err != nil {
			return xerrors.Errorf("failed to connect to witness: %v", err)
		}
	}

	go hub.CreateAndServeWs(hub.WitnessHubType, hub.ClientSocketType, h, clientPort)
	go hub.CreateAndServeWs(hub.WitnessHubType, hub.WitnessSocketType, h, witnessPort)

	done := make(chan struct{})
	h.Start(done)

	done <- struct{}{}

	return nil
}

func connectToSocket(socketType hub.SocketType, address string, h hub.Hub, port int) error {
	var urlString = ""
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
		return xerrors.Errorf("failed to parse connection url %s %v", url, err)
	}
	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("dialing, %v", err)
	}
	log.Printf("connected to %s at %s", socketType, urlString)
	switch socketType {
	case hub.OrganizerSocketType:
		organizerSocket := hub.NewOrganizerSocket(h, ws)
		go organizerSocket.WritePump()
		go organizerSocket.ReadPump()
	case hub.WitnessSocketType:
		witnessSocket := hub.NewWitnessSocket(h, ws)
		go witnessSocket.WritePump()
		go witnessSocket.ReadPump()
	}

	return nil
}
