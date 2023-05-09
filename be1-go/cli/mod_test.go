package main

import (
	"io"
	"popstellar/channel/lao"
	"popstellar/crypto"
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

	oh, err := standard_hub.NewHub(crypto.Suite.Point(), "", "", log, lao.NewChannel)
	require.NoError(t, err)
	oh.Start()

	remoteSrv := network.NewServer(oh, "localhost", 9001, socket.ServerSocketType, log)
	remoteSrv.Start()
	<-remoteSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := standard_hub.NewHub(crypto.Suite.Point(), "", "", log, lao.NewChannel)
	require.NoError(t, err)
	wDone := make(chan struct{})
	wh.Start()

	wg := &sync.WaitGroup{}
	err = connectToSocket("localhost:9001", wh, wg, wDone, "")
	require.NoError(t, err)

	err = remoteSrv.Shutdown()
	require.NoError(t, err)
	<-remoteSrv.Stopped

	oh.Stop()
	wh.Stop()
	close(wDone)
	wg.Wait()
}
