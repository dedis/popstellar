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

	"github.com/stretchr/testify/require"
)

func TestCreateAndServeWS(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h, err := hub.NewWitnessHub(student20_pop.Suite.Point())
	require.NoErrorf(t, err, "could not create witness hub")

	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	srv := CreateAndServeWS(ctx, "testhub", "testsocket", h, 9000, wg)
	str := buffer.String()
	condition := strings.Contains(str, "Starting the testhub WS server (for testsocket) at 9000")
	require.Truef(t, condition, "server not starting: %s", str)

	srv.Shutdown(ctx)
	wg.Wait()

	str = buffer.String()
	condition = strings.Contains(str, "stopped the server...")
	require.Truef(t, condition, "failed to stop the server: %s", str)
}
