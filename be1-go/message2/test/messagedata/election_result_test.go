package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Result(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_result.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "result", action)

	var msg messagedata.ElectionResult

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "result", msg.Action)

	require.Len(t, msg.Questions, 1)
	require.Equal(t, "XXX", msg.Questions[0].ID)

	require.Len(t, msg.Questions[0].Result, 1)
	require.Equal(t, "XXX", msg.Questions[0].Result[0].BallotOption)
	require.Equal(t, 123, msg.Questions[0].Result[0].Count)
}
