package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata/lao/mlao"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Setup(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_setup", "election_setup.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := mmessage.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "setup", action)

	var msg mlao.ElectionSetup

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "setup", msg.Action)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.ID)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "Election", msg.Name)
	require.Equal(t, "OPEN_BALLOT", msg.Version)
	require.Equal(t, int64(1633098941), msg.CreatedAt)
	require.Equal(t, int64(1633098941), msg.StartTime)
	require.Equal(t, int64(1633099812), msg.EndTime)

	require.Len(t, msg.Questions, 1)
	require.Equal(t, "2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=", msg.Questions[0].ID)
	require.Equal(t, "Is this project fun?", msg.Questions[0].Question)
	require.Equal(t, "Plurality", msg.Questions[0].VotingMethod)

	require.Len(t, msg.Questions[0].BallotOptions, 2)
	require.Equal(t, "Yes", msg.Questions[0].BallotOptions[0])
	require.Equal(t, "No", msg.Questions[0].BallotOptions[1])
	require.Equal(t, false, msg.Questions[0].WriteIn)
}

func Test_Election_Setup_Interface_Functions(t *testing.T) {
	var msg mlao.ElectionSetup

	require.Equal(t, mmessage.ElectionObject, msg.GetObject())
	require.Equal(t, mmessage.ElectionActionSetup, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
