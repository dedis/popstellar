package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message"
	"popstellar/message/query/method"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Unsubscribe(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "unsubscribe", "unsubscribe.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message.RPCTypeQuery, rpctype)

	var unsubscribe method.Unsubscribe

	err = json.Unmarshal(buf, &unsubscribe)
	require.NoError(t, err)

	require.Equal(t, "unsubscribe", unsubscribe.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", unsubscribe.Params.Channel)
	require.Equal(t, 7, unsubscribe.ID)
}
