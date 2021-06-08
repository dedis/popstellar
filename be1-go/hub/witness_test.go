package hub

import (
	"bytes"
	"context"
	"encoding/base64"
	"log"
	"strings"
	"student20_pop"
	"sync"
	"testing"
)

func CreateWitnessHub() Hub {
	pk := "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		return nil
	}
	point := student20_pop.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		return nil
	}
	return NewWitnessHub(point)
}

// to complete
func TestNewWitnessHub(t *testing.T) {
	pk := "invalid pk"
	_, err := base64.URLEncoding.DecodeString(pk)
	if (err == nil) {
		t.Errorf("decoded invalid string")
	}
	pk = "OgFFZz2TVilTSICEdJbAO3otWGfh17SmPo6i5as7XAg="
	pkBuf, err := base64.URLEncoding.DecodeString(pk)
	if err != nil {
		t.Errorf("could not decode public key")
	}
	point := student20_pop.Suite.Point()
	err = point.UnmarshalBinary(pkBuf)
	if err != nil {
		t.Errorf("could not unmarshal public key")
	}
	NewWitnessHub(point)
}

func TestWitnessHub_Start(t *testing.T) {
	parent := context.Background()
	ctx, cancel := context.WithCancel(parent)
	wg := &sync.WaitGroup{}
	witnessHub := CreateWitnessHub()
	if witnessHub == nil {
		t.Errorf("could not create witness hub")
	}

	var buffer bytes.Buffer
	log.SetOutput(&buffer)

	cancel()
	witnessHub.Start(ctx, wg)

	condition := strings.Contains(buffer.String(), "started witness...") && strings.Contains(buffer.String(), "closing the hub...")

	if (!condition) {
		t.Errorf("wrong strings logged in witnessHub start")
	}
}