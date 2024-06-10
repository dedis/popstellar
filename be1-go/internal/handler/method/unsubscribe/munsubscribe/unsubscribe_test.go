package munsubscribe

import (
	"embed"
	"encoding/json"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Unsubscribe(t *testing.T) {
	buf, err := testData.ReadFile("testdata/unsubscribe.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, mjsonrpc.RPCTypeQuery, rpctype)

	var unsubscribe Unsubscribe

	err = json.Unmarshal(buf, &unsubscribe)
	require.NoError(t, err)

	require.Equal(t, "unsubscribe", unsubscribe.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", unsubscribe.Params.Channel)
	require.Equal(t, 7, unsubscribe.ID)
}
