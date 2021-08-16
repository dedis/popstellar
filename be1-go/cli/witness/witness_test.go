package witness

import (
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
	oh, err := hub.NewOrganizerHub(crypto.Suite.Point())
	require.NoError(t, err)
	oh.Start()

	witnessSrv := network.NewServer(oh, 9001, socket.WitnessSocketType)
	witnessSrv.Start()
	<-witnessSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := hub.NewWitnessHub(crypto.Suite.Point())
	require.NoError(t, err)
	wDone := make(chan struct{})
	wh.Start()

	wg := &sync.WaitGroup{}
	err = connectToWitnessSocket(hub.OrganizerHubType, "localhost:9001", wh, wg, wDone)
	require.NoError(t, err)

	err = witnessSrv.Shutdown()
	require.NoError(t, err)
	<-witnessSrv.Stopped

	oh.Stop()
	wh.Stop()
	close(wDone)
	wg.Wait()
}
