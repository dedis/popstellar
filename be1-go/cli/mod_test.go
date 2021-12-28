package main

import (
	"io"
	"popstellar/channel"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
)

func TestConnectToSocket(t *testing.T) {
	log := zerolog.New(io.Discard)

	oh, err := newMockHub(crypto.Suite.Point(), log, lao.NewChannel, hub.OrganizerHubType)
	require.NoError(t, err)
	oh.Start()

	witnessSrv := network.NewServer(oh, 9001, socket.WitnessSocketType, log)
	witnessSrv.Start()
	<-witnessSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := newMockHub(crypto.Suite.Point(), log, lao.NewChannel, hub.WitnessHubType)
	require.NoError(t, err)
	wDone := make(chan struct{})
	wh.Start()

	wg := &sync.WaitGroup{}
	err = connectToSocket(hub.OrganizerHubType, "localhost:9001", wh, wg, wDone)
	require.NoError(t, err)

	err = witnessSrv.Shutdown()
	require.NoError(t, err)
	<-witnessSrv.Stopped

	oh.Stop()
	wh.Stop()
	close(wDone)
	wg.Wait()
}

// mockHub implements the Hub interface.
type mockHub struct {
	hubType hub.HubType

	messageChan chan socket.IncomingMessage

	closedSockets chan string

	pubKeyOrg kyber.Point

	stop chan struct{}

	laoFac channel.LaoFactory
}

// newMockHub returns a new mocked Hub.
func newMockHub(publicOrg kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory,
	hubType hub.HubType) (*mockHub, error) {

	hub := mockHub{
		hubType:       hubType,
		messageChan:   make(chan socket.IncomingMessage),
		closedSockets: make(chan string),
		pubKeyOrg:     publicOrg,
		stop:          make(chan struct{}),
		laoFac:        laoFac,
	}

	return &hub, nil
}

func (h *mockHub) NotifyNewServer(socket.Socket) error {
	return nil
}

func (h *mockHub) Start() {
	go func() {
		<-h.stop
	}()
}

func (h *mockHub) Stop() {
	close(h.stop)
}

func (h *mockHub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

func (h *mockHub) OnSocketClose() chan<- string {
	return h.closedSockets
}

func (h *mockHub) Type() hub.HubType {
	return h.hubType
}
