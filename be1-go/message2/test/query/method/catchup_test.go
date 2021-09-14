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

func Test_Catchup(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "catchup", "catchup.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message2.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message2.RPCTypeQuery, rpctype)

	var catchup method.Catchup

	err = json.Unmarshal(buf, &catchup)
	require.NoError(t, err)

	require.Equal(t, "catchup", catchup.Method)
	require.Equal(t, "/root/XXX", catchup.Params.Channel)
}
