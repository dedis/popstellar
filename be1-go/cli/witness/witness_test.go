package witness

import (
	"context"
	"student20_pop/crypto"
	"student20_pop/hub"
	"student20_pop/network"
	"student20_pop/network/socket"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestConnectToWitnessSocket(t *testing.T) {
	parent := context.Background()
	ctx, cancel := context.WithCancel(parent)
	wg := &sync.WaitGroup{}

	oh, err := hub.NewOrganizerHub(crypto.Suite.Point(), wg)
	require.NoError(t, err)
	go oh.Start(ctx)

	witnessSrv := network.NewServer(ctx, oh, 9001, socket.WitnessSocketType, wg)
	witnessSrv.Start()
	<-witnessSrv.Started

	time.Sleep(1 * time.Second)

	wg2 := &sync.WaitGroup{}
	whCtx, whCancel := context.WithCancel(parent)
	wh, err := hub.NewWitnessHub(crypto.Suite.Point(), wg2)
	require.NoError(t, err)
	go wh.Start(whCtx)

	err = connectToWitnessSocket(ctx, hub.OrganizerHubType, "localhost:9001", wh, wg2)
	require.NoError(t, err)

	err = witnessSrv.Shutdown()
	require.NoError(t, err)
	<-witnessSrv.Stopped

	cancel()
	wg.Wait()

	whCancel()
	wg2.Wait()
}
