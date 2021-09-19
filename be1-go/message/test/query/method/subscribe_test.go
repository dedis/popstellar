package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message"
	"student20_pop/message/query/method"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Subscribe(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "subscribe", "subscribe.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, rpctype, message.RPCTypeQuery)

	var subscribe method.Subscribe

	err = json.Unmarshal(buf, &subscribe)
	require.NoError(t, err)

	require.Equal(t, "subscribe", subscribe.Method)
	require.Equal(t, "/root/XXX", subscribe.Params.Channel)
	require.Equal(t, 999, subscribe.ID)
}
