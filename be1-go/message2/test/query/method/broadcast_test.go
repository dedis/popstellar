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

func Test_Broadcast(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "broadcast", "broadcast.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message2.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message2.RPCTypeQuery, rpctype)

	var broadcast method.Broadcast

	err = json.Unmarshal(buf, &broadcast)
	require.NoError(t, err)

	require.Equal(t, "broadcast", broadcast.Method)
	require.Equal(t, "/root/XXX", broadcast.Params.Channel)
}
