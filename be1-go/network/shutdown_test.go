package network

import (
	"context"
	"student20_pop/crypto"
	"student20_pop/hub"
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestShutdownServers(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h, err := hub.NewWitnessHub(crypto.Suite.Point(), wg)
	require.NoError(t, err)

	// Start the servers up
	witnessSrv := NewServer(ctx, h, 9000, hub.WitnessSocketType, wg)
	witnessSrv.Start()
	<-witnessSrv.Started

	clientSrv := NewServer(ctx, h, 9001, hub.ClientSocketType, wg)
	clientSrv.Start()
	<-clientSrv.Started

	// Shut them down
	witnessSrv.Shutdown()
	<-witnessSrv.Stopped

	clientSrv.Shutdown()
	<-clientSrv.Stopped

	wg.Wait()
}
