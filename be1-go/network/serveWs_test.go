package network

import (
	"bytes"
	"context"
	"log"
	"strings"
	"student20_pop"
	"student20_pop/hub"
	"sync"
	"testing"
)

func TestCreateAndServeWS(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h := hub.NewWitnessHub(student20_pop.Suite.Point())

	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	srv := CreateAndServeWS(ctx, "testhub", "testsocket", h, 9000, wg)
	str := buffer.String()
	condition := strings.Contains(str, "Starting the testhub WS server (for testsocket) at 9000")
	if !condition {
		t.Errorf("server not starting")
	}

	srv.Shutdown(ctx)
	wg.Wait()

	str = buffer.String()
	condition = strings.Contains(str, "stopped the server...")
	if !condition {
		t.Errorf("servers not stopping")
	}
}