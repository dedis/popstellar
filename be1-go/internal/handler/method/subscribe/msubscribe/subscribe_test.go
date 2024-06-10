package msubscribe

import (
	"embed"
	"encoding/json"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Subscribe(t *testing.T) {
	buf, err := testData.ReadFile("testdata/subscribe.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, rpctype, mjsonrpc.RPCTypeQuery)

	var subscribe Subscribe

	err = json.Unmarshal(buf, &subscribe)
	require.NoError(t, err)

	require.Equal(t, "subscribe", subscribe.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", subscribe.Params.Channel)
	require.Equal(t, 2, subscribe.ID)
}
