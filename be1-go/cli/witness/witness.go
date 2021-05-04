package witness

import (
	"encoding/base64"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
	"net/http"
	"net/url"
	"student20_pop"
	"student20_pop/hub"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func Serve(context *cli.Context) error {
	organizerAddr := context.String("organizer-address")
	organizerPort := context.Int("organizer-port")
	pk := context.String("public-key")

	if pk == "" {
		return xerrors.Errorf("witness' public key is required")
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

	h := hub.NewWitnessHub(point)

	ws, err := connectToOrganizer(organizerAddr, organizerPort)
	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	organizerSocket := hub.NewOrganizerSocket(h, ws)

	go organizerSocket.WritePump()
	go organizerSocket.ReadPump()

	done := make(chan struct{})
	h.Start(done)

	//TODO: connect to clients and other witnesses

	done <- struct{}{}

	return nil
}

func connectToOrganizer(organizerAddr string, port int) (*websocket.Conn, error) {
	u, err := url.Parse(fmt.Sprintf("ws://%s:%d/organizer/witness/", organizerAddr, port))
	if err != nil {
		return nil, xerrors.Errorf("failure to connect to organizer: %v", err)
	}
	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return ws, xerrors.Errorf("failure to connect to organizer: %v", err)
	}
	return ws, nil
}
