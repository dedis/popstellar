package network

import (
	"bytes"
	"context"
	"log"
	"student20_pop/crypto"
	"student20_pop/hub"
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCreateAndServeWS(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h, err := hub.NewWitnessHub(crypto.Suite.Point(), wg)
	require.NoErrorf(t, err, "could not create witness hub")

	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	srv := NewServer(ctx, h, 9000, "testsocket", wg)
	srv.Start()
	<-srv.Started

	srv.Shutdown()
	<-srv.Stopped

	wg.Wait()
}
