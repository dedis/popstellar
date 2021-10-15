package witness

import (
	"encoding/base64"
	"io"
	"popstellar/crypto"
	"popstellar/hub"
	"testing"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func createWitnessHub() (hub.Hub, error) {
	log := zerolog.New(io.Discard)

	pk := "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		return nil, err
	}

	point := crypto.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		return nil, err
	}

	h, err := NewHub(point, log, nil)
	if err != nil {
		return nil, err
	}

	return h, nil
}

// to complete
func TestNewWitnessHub(t *testing.T) {
	log := zerolog.New(io.Discard)

	pk := "invalid pk"
	_, err := base64.URLEncoding.DecodeString(pk)
	require.Error(t, err)

	pk = "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	require.NoError(t, err)

	point := crypto.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	require.NoError(t, err)

	_, err = NewHub(point, log, nil)
	require.NoError(t, err)
}

func TestWitnessHub_Start(t *testing.T) {
	witnessHub, err := createWitnessHub()
	require.NoError(t, err)

	witnessHub.Start()
	witnessHub.Stop()
}
