package answer

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Answer_General(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "general_empty.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message2.JSONRPC

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// > should be of type "answer"
	require.Equal(t, message2.RPCTypeAnswer, msg.Type())

	// > result type should be empty
	require.True(t, msg.Result.IsEmpty())

	// > should contain the expected elements
	require.Equal(t, 999, msg.ID)
	require.Equal(t, "2.0", msg.JSONRPC)
}
