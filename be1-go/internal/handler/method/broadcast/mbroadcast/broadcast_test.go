package mbroadcast

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Broadcast(t *testing.T) {
	buf, err := testData.ReadFile("testdata/broadcast.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, mjsonrpc.RPCTypeQuery, rpctype)

	var broadcast Broadcast

	err = json.Unmarshal(buf, &broadcast)
	require.NoError(t, err)

	require.Equal(t, "broadcast", broadcast.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", broadcast.Params.Channel)
}
