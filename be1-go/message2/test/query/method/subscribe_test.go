package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Subscribe(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "subscribe", "subscribe.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPC

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// > should be of type "query"
	require.Equal(t, message2.RPCTypeQuery, msg.Type())

	require.Equal(t, "subscribe", msg.Method)
	require.Equal(t, "/root/XXX", msg.Subscribe.Params.Channel)
	require.Equal(t, 999, msg.Subscribe.ID)
}
