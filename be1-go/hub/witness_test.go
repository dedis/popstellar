package hub

import (
	"context"
	"encoding/base64"
	"student20_pop/crypto"
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
)

func createWitnessHub(wg *sync.WaitGroup) (Hub, error) {
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
	h, err := NewWitnessHub(point, wg)
	if err != nil {
		return nil, err
	}
	return h, nil
}

// to complete
func TestNewWitnessHub(t *testing.T) {
	pk := "invalid pk"
	_, err := base64.URLEncoding.DecodeString(pk)
	require.Error(t, err)

	pk = "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	require.NoError(t, err)

	point := crypto.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	require.NoError(t, err)

	_, err = NewWitnessHub(point, &sync.WaitGroup{})
	require.NoError(t, err)
}

func TestWitnessHub_Start(t *testing.T) {
	parent := context.Background()
	ctx, cancel := context.WithCancel(parent)
	wg := &sync.WaitGroup{}
	witnessHub, err := createWitnessHub(wg)
	require.NoError(t, err)

	go witnessHub.Start(ctx)
	cancel()

	// this checks if the start loop exits cleanly on context cancel
	wg.Wait()
}
