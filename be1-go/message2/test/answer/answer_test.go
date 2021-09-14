package answer

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2"
	"student20_pop/message2/answer"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Answer_General(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "general_empty.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message2.GetType(buf)
	require.NoError(t, err)

	// > should be of type "answer"
	require.Equal(t, message2.RPCTypeAnswer, rpctype)

	var answer answer.Answer

	err = json.Unmarshal(buf, &answer)
	require.NoError(t, err)

	// > result type should be empty
	require.True(t, answer.Result.IsEmpty())

	// > should contain the expected elements
	require.Equal(t, 999, answer.ID)
	require.Equal(t, "2.0", answer.JSONRPC)
}
