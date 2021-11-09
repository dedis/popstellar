package network

import (
	"io"
	"popstellar/crypto"
	"popstellar/hub"
	hub_impl "popstellar/hub/hub"
	"testing"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestServerStartAndShutdown(t *testing.T) {
	log := zerolog.New(io.Discard)

	h, err := hub_impl.NewHub(crypto.Suite.Point(), log, nil, hub.WitnessHubType)
	require.NoErrorf(t, err, "could not create witness hub")

	srv := NewServer(h, 0, "testsocket", log)
	srv.Start()
	<-srv.Started

	err = srv.Shutdown()
	require.NoError(t, err)
	<-srv.Stopped
}
