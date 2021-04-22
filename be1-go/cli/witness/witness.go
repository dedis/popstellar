package witness

import (
	"encoding/base64"
	"flag"
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

func WitnessServe(context *cli.Context) error {
	orgIpAddr := context.String("ip-address")
	port := context.Int("port")
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

	ws, err := witConnectToOrganizer(orgIpAddr, port)
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

func witConnectToOrganizer(orgIpAddr string, port int) (*websocket.Conn, error){
	var addr = flag.String("addr",fmt.Sprintf("%s:%d", orgIpAddr, port), "http service address")
	u := url.URL{Scheme: "ws", Host: *addr, Path: "/org/witness/"}
	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return ws, xerrors.Errorf("failed to connect to organizer: %v", err)
	}
	return ws, nil
}