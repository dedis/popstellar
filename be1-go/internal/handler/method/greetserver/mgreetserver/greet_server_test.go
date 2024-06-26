package mgreetserver

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_GreetServer(t *testing.T) {
	buf, err := testData.ReadFile("testdata/greet_server.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, mjsonrpc.RPCTypeQuery, rpctype)

	var greetServer GreetServer

	err = json.Unmarshal(buf, &greetServer)
	require.NoError(t, err)

	require.Equal(t, "greet_server", greetServer.Method)
	require.Equal(t, "2.0", greetServer.JSONRPC)

	serverInfo := greetServer.Params

	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", serverInfo.PublicKey)
	require.Equal(t, "wss://popdemo.dedis.ch:9001/server", serverInfo.ServerAddress)
	require.Equal(t, "wss://popdemo.dedis.ch:9000/client", serverInfo.ClientAddress)
}
