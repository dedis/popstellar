package melection

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Result(t *testing.T) {
	buf, err := testData.ReadFile("testdata/election_result.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "result", action)

	var msg ElectionResult

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "result", msg.Action)

	require.Len(t, msg.Questions, 1)
	require.Equal(t, "2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=", msg.Questions[0].ID)

	require.Len(t, msg.Questions[0].Result, 2)
	require.Equal(t, "Yes", msg.Questions[0].Result[0].BallotOption)
	require.Equal(t, 1, msg.Questions[0].Result[0].Count)
	require.Equal(t, "No", msg.Questions[0].Result[1].BallotOption)
	require.Equal(t, 0, msg.Questions[0].Result[1].Count)
}

func Test_Election_Result_Interface_Functions(t *testing.T) {
	var msg ElectionResult

	require.Equal(t, channel.ElectionObject, msg.GetObject())
	require.Equal(t, channel.ElectionActionResult, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
