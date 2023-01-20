package network

import (
	"io"
	"net/http"
	"net/http/httptest"
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

func TestInfoHandler(t *testing.T) {
	s := Server{
		hub: fakeHub{},
		st:  "fake",
	}

	rr := httptest.NewRecorder()
	s.infoHandler(rr, &http.Request{Method: http.MethodGet})

	res, err := io.ReadAll(rr.Body)
	require.NoError(t, err)

	expected := `{
	"version": "unknown",
	"commit": "unknown",
	"buildTime": "unknown",
	"hubType": "fake",
	"socketType": "fake"
}`

	require.Equal(t, expected, string(res))
}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHub struct {
	hub.Hub
}

func (fakeHub) Type() hub.HubType {
	return "fake"
}
