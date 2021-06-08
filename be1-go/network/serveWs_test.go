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
	hub := hub.NewWitnessHub(student20_pop.Suite.Point())
	wg := &sync.WaitGroup{}

	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	srv := CreateAndServeWS(ctx, "witness", "client", hub, 9000, wg)
	srv.Shutdown(ctx)

	str := buffer.String()

	condition := strings.Contains(str, "Starting the witness WS server (for client) at 9000")

	if !condition {
		t.Errorf("server not starting")
	}
}