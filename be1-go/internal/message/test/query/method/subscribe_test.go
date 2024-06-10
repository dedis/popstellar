package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/message/method/msubscribe"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Subscribe(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "subscribe", "subscribe.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, rpctype, mjsonrpc.RPCTypeQuery)

	var subscribe msubscribe.Subscribe

	err = json.Unmarshal(buf, &subscribe)
	require.NoError(t, err)

	require.Equal(t, "subscribe", subscribe.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", subscribe.Params.Channel)
	require.Equal(t, 2, subscribe.ID)
}
