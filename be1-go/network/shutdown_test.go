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

func TestShutdownServers(t *testing.T) {
	ctx := context.Background()
	wg := &sync.WaitGroup{}

	h, err := hub.NewWitnessHub(student20_pop.Suite.Point())
	require.NoError(t, err)

	witnessSrv := CreateAndServeWS(ctx, hub.WitnessHubType, hub.WitnessSocketType, h, 9000, wg)
	clientSrv := CreateAndServeWS(ctx, hub.WitnessHubType, hub.ClientSocketType, h, 9000, wg)

	buffer := bytes.Buffer{}
	log.SetOutput(&buffer)

	shutdownServers(ctx, witnessSrv, clientSrv)

	wg.Wait()

	str := buffer.String()
	condition := strings.Contains(str, "shutdown both servers") && !strings.Contains(str, "failed")
	require.Truef(t, condition, "failed to correctly shutdown both servers: %s", str)
}
