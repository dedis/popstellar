package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2"
	"student20_pop/message2/query/method"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Unsubscribe(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "unsubscribe", "unsubscribe.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message2.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message2.RPCTypeQuery, rpctype)

	var unsubscribe method.Unsubscribe

	err = json.Unmarshal(buf, &unsubscribe)
	require.NoError(t, err)

	require.Equal(t, "unsubscribe", unsubscribe.Method)
	require.Equal(t, "/root/XXX", unsubscribe.Params.Channel)
	require.Equal(t, 999, unsubscribe.ID)
}
