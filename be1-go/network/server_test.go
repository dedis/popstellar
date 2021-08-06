package network

import (
	"context"
	"student20_pop/crypto"
	"student20_pop/hub"
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestServerStartAndShutdown(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h, err := hub.NewWitnessHub(crypto.Suite.Point(), wg)
	require.NoErrorf(t, err, "could not create witness hub")

	srv := NewServer(ctx, h, 9000, "testsocket", wg)
	srv.Start()
	<-srv.Started

	err = srv.Shutdown()
	require.NoError(t, err)
	<-srv.Stopped

	wg.Wait()
}
