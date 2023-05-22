package method

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message"
	"popstellar/message/query/method"
	"testing"
)

func Test_GreetServer(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "greet_server", "greet_server.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message.RPCTypeQuery, rpctype)

	var greetServer method.GreetServer

	err = json.Unmarshal(buf, &greetServer)
	require.NoError(t, err)

	require.Equal(t, "greet_server", greetServer.Method)
	require.Equal(t, "2.0", greetServer.JSONRPC)

	serverInfo := greetServer.Params

	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", serverInfo.PublicKey)
	require.Equal(t, "wss://popdemo.dedis.ch:9001/server", serverInfo.ServerAddress)
	require.Equal(t, "wss://popdemo.dedis.ch:9000/client", serverInfo.ClientAddress)
}
