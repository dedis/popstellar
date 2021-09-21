package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Setup(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_setup.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "setup", action)

	var msg messagedata.ElectionSetup

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "setup", msg.Action)
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Lao)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, "XXX", msg.Version)
	require.Equal(t, int64(123), msg.CreatedAt)
	require.Equal(t, int64(123), msg.StartTime)
	require.Equal(t, int64(123), msg.EndTime)

	require.Len(t, msg.Questions, 1)
	require.Equal(t, "XXX", msg.Questions[0].ID)
	require.Equal(t, "XXX", msg.Questions[0].Question)
	require.Equal(t, "Plurality", msg.Questions[0].VotingMethod)

	require.Len(t, msg.Questions[0].BallotOptions, 2)
	require.Equal(t, "XXX", msg.Questions[0].BallotOptions[0])
	require.Equal(t, "YYY", msg.Questions[0].BallotOptions[1])
	require.Equal(t, false, msg.Questions[0].WriteIn)
}
