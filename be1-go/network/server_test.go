package network

import (
	"io"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"testing"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestServerStartAndShutdown(t *testing.T) {
	log := zerolog.New(io.Discard)

	h, err := standard_hub.NewHub(crypto.Suite.Point(), "", log, nil, hub.WitnessHubType)
	require.NoErrorf(t, err, "could not create witness hub")

	srv := NewServer(h, "", 0, "testsocket", log)
	srv.Start()
	<-srv.Started

	err = srv.Shutdown()
	require.NoError(t, err)
	<-srv.Stopped
}
