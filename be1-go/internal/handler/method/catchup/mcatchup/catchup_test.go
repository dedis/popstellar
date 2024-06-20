package mcatchup

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Catchup(t *testing.T) {
	buf, err := testData.ReadFile("testdata/catchup.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, mjsonrpc.RPCTypeQuery, rpctype)

	var catchup Catchup

	err = json.Unmarshal(buf, &catchup)
	require.NoError(t, err)

	require.Equal(t, "catchup", catchup.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", catchup.Params.Channel)
}
