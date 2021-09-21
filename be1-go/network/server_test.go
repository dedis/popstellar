package network

import (
	"io"
	"student20_pop/crypto"
	"student20_pop/hub"
	"testing"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestServerStartAndShutdown(t *testing.T) {
	log := zerolog.New(io.Discard)

	h, err := hub.NewWitnessHub(crypto.Suite.Point(), log)
	require.NoErrorf(t, err, "could not create witness hub")

	srv := NewServer(h, 9000, "testsocket", log)
	srv.Start()
	<-srv.Started

	err = srv.Shutdown()
	require.NoError(t, err)
	<-srv.Stopped
}
