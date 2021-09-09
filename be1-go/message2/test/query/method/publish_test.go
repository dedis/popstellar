package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Publish(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "publish", "publish.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPC

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// > should be of type "query"
	require.Equal(t, message2.RPCTypeQuery, msg.Type())

	require.Equal(t, "publish", msg.Method)
	require.Equal(t, "/root/XXX", msg.Publish.Params.Channel)
	require.Equal(t, 999, msg.Publish.ID)
	require.Equal(t, "XXX", msg.Publish.Params.Message.Data)
	require.Equal(t, "XXX", msg.Publish.Params.Message.Sender)
	require.Equal(t, "XXX", msg.Publish.Params.Message.Signature)
	require.Equal(t, "XXX", msg.Publish.Params.Message.MessageID)
	require.Len(t, msg.Publish.Params.Message.WitnessSignatures, 0)
}
