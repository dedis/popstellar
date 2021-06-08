package witness

import (
	"bytes"
	"context"
	"log"
	"strings"
	"student20_pop"
	"student20_pop/hub"
	"student20_pop/network"
	"sync"
	"testing"
)

func TestServe(t *testing.T) {
	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	parent := context.Background()
	ctx, cancel := context.WithCancel(parent)
	wg := &sync.WaitGroup{}
	h := hub.NewWitnessHub(student20_pop.Suite.Point())
	witnessSrv := network.CreateAndServeWS(ctx, hub.WitnessHubType, hub.WitnessSocketType, h, 9000, wg)

	err := connectToSocket(ctx, hub.WitnessSocketType, "localhost:9000", h, 9000, wg)
	if err != nil {
		t.Errorf("unable to connect to server")
	}

	witnessSrv.Shutdown(ctx)

	cancel()
	wg.Wait()

	str := buffer.String()
	condition := strings.Contains(str, "connected to witness at ws://localhost:9000/witness/witness/")
	condition = condition && strings.Contains(str, "listening for messages from witness")
	if !condition {
		t.Errorf("unable to connect")
	}
}