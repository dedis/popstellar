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

func Test_Publish(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "publish", "publish.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message.RPCTypeQuery, rpctype)

	var publish method.Publish

	err = json.Unmarshal(buf, &publish)
	require.NoError(t, err)

	require.Equal(t, "publish", publish.Method)
	require.Equal(t, "/root/XXX", publish.Params.Channel)
	require.Equal(t, 999, publish.ID)
	require.Equal(t, "XXX", publish.Params.Message.Data)
	require.Equal(t, "XXX", publish.Params.Message.Sender)
	require.Equal(t, "XXX", publish.Params.Message.Signature)
	require.Equal(t, "XXX", publish.Params.Message.MessageID)
	require.Len(t, publish.Params.Message.WitnessSignatures, 0)
}
