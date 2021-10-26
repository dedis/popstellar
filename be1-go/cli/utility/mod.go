package utility

import (
	"fmt"
	"net/url"

	be1_go "popstellar"
	"popstellar/hub"
	"popstellar/network/socket"
	"sync"

	"github.com/gorilla/websocket"
	"golang.org/x/xerrors"
)

// ConnectToSocket establishes a connection to another server's witness
// endpoint.
func ConnectToSocket(otherHubType hub.HubType, address string, h hub.Hub, wg *sync.WaitGroup, done chan struct{}) error {
	log := be1_go.Logger

	urlString := fmt.Sprintf("ws://%s/%s/witness", address, otherHubType)
	u, err := url.Parse(urlString)
	if err != nil {
		return xerrors.Errorf("failed to parse connection url %s %v", urlString, err)
	}

	ws, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return xerrors.Errorf("failed to dial to %s: %v", u.String(), err)
	}

	log.Info().Msgf("connected to %s at %s", otherHubType, urlString)

	switch otherHubType {
	case hub.OrganizerHubType:
		organizerSocket := socket.NewOrganizerSocket(h.Receiver(),
			h.OnSocketClose(), ws, wg, done, log)
		wg.Add(2)

		go organizerSocket.WritePump()
		go organizerSocket.ReadPump()
	case hub.WitnessHubType:
		witnessSocket := socket.NewWitnessSocket(h.Receiver(),
			h.OnSocketClose(), ws, wg, done, log)
		wg.Add(2)

		go witnessSocket.WritePump()
		go witnessSocket.ReadPump()
	default:
		return xerrors.Errorf("invalid other hub type: %v", otherHubType)
	}

	return nil
}
