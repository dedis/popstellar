package witness

import (
	"encoding/base64"
	"flag"
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

func WitnessServe(context *cli.Context) error {
	//port := context.Int("port")
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

	var addr = flag.String("addr", "localhost:8080", "http service address")
	u := url.URL{Scheme: "ws", Host: *addr, Path: "/echo"}
	var ws *websocket.Conn
	ws, _, err = websocket.DefaultDialer.Dial(u.String(), nil)

	if err != nil {
		return xerrors.Errorf("failed to connect to organizer: %v", err)
	}

	organizerSocket := hub.NewOrganizerSocket(h, ws)

	organizerSocket.WritePump()
	organizerSocket.ReadPump()

	return nil
}
