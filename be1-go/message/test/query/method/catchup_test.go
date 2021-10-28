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

func Test_Catchup(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "catchup", "catchup.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message.RPCTypeQuery, rpctype)

	var catchup method.Catchup

	err = json.Unmarshal(buf, &catchup)
	require.NoError(t, err)

	require.Equal(t, "catchup", catchup.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", catchup.Params.Channel)
}
