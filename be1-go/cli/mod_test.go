package main

import (
	"io"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestConnectToSocket(t *testing.T) {
	log := zerolog.New(io.Discard)

	oh, err := standard_hub.NewHub(crypto.Suite.Point(), "", log, lao.NewChannel, hub.OrganizerHubType, true)
	require.NoError(t, err)
	oh.Start()

	witnessSrv := network.NewServer(oh, "localhost", 9001, socket.WitnessSocketType, log)
	witnessSrv.Start()
	<-witnessSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := standard_hub.NewHub(crypto.Suite.Point(), "", log, lao.NewChannel, hub.WitnessHubType, true)
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
