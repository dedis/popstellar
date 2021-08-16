package network

import (
	"student20_pop/crypto"
	"student20_pop/hub"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestServerStartAndShutdown(t *testing.T) {
	h, err := hub.NewWitnessHub(crypto.Suite.Point())
	require.NoErrorf(t, err, "could not create witness hub")

	srv := NewServer(h, 9000, "testsocket")
	srv.Start()
	<-srv.Started

	err = srv.Shutdown()
	require.NoError(t, err)
	<-srv.Stopped
}
