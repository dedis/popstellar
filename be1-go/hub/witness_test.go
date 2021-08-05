package hub

import (
	"bytes"
	"context"
	"encoding/base64"
	"log"
	"strings"
	"student20_pop/crypto"
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
)

func CreateWitnessHub(wg *sync.WaitGroup) Hub {
	pk := "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		return nil
	}
	point := crypto.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		return nil
	}
	h, err := NewWitnessHub(point, wg)
	if err != nil {
		return nil
	}
	return h
}

// to complete
func TestNewWitnessHub(t *testing.T) {
	pk := "invalid pk"
	_, err := base64.URLEncoding.DecodeString(pk)
	require.NoError(t, err)

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
	witnessHub := CreateWitnessHub(wg)
	if witnessHub == nil {
		t.Errorf("could not create witness hub")
	}

	var buffer bytes.Buffer
	log.SetOutput(&buffer)

	cancel()
	witnessHub.Start(ctx)

	condition := strings.Contains(buffer.String(), "started witness...") && strings.Contains(buffer.String(), "closing the hub...")

	if !condition {
		t.Errorf("wrong strings logged in witnessHub start")
	}
}
