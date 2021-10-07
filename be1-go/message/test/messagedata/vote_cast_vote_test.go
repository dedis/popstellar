package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Vote_Cast_Vote(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "vote_cast_vote.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "cast_vote", action)

	var msg messagedata.VoteCastVote

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "cast_vote", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "JVehtpaFJwPc2dp8VuiskXwxmjTQGT6OiIVbei-EbBI=", msg.Election)
	require.Equal(t, int64(1633098941), msg.CreatedAt)

	require.Len(t, msg.Votes, 1)
	require.Equal(t, "JF8pLNhK6BArJpZxL7IWhbC6doPDpGPClRkgav0ry_0=", msg.Votes[0].ID)
	require.Equal(t, "AQy6JrPiLXG7oIZ_EL83hbs87w_vjn5ZS3r4je1wV8o=", msg.Votes[0].Question)

	require.Len(t, msg.Votes[0].Vote, 1)
	require.Equal(t, 0, msg.Votes[0].Vote[0])
}
