package melection

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

func Test_Vote_Cast_Vote(t *testing.T) {
	buf, err := testData.ReadFile("testdata/vote_cast_vote.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "cast_vote", action)

	var msg VoteCastVote

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "cast_vote", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, int64(1633098941), msg.CreatedAt)

	require.Len(t, msg.Votes, 1)
	require.Equal(t, "8L2MWJJYNGG57ZOKdbmhHD9AopvBaBN26y1w5jL07ms=", msg.Votes[0].ID)
	require.Equal(t, "2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=", msg.Votes[0].Question)

	require.Equal(t, 0, msg.Votes[0].Vote)
}

func Test_Vote_Cast_Vote_Interface_Functions(t *testing.T) {
	var msg VoteCastVote

	require.Equal(t, channel.ElectionObject, msg.GetObject())
	require.Equal(t, channel.VoteActionCastVote, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Cast_Vote_UnmarshalJSON(t *testing.T) {
	testWithWrongType := func(obj interface{}) func(*testing.T) {
		return func(t *testing.T) {
			buf, err := testData.ReadFile("testdata/vote_cast_vote.json")
			require.NoError(t, err)

			var msg VoteCastVote

			err = json.Unmarshal(buf, &msg)
			require.NoError(t, err)

			msg.Votes[0].Vote = obj

			buf, err = json.Marshal(msg)
			require.NoError(t, err)

			err = json.Unmarshal(buf, &msg)
			require.Error(t, err)
		}
	}

	t.Run("vote is an array", testWithWrongType([]int{0}))
	t.Run("vote is a boolean", testWithWrongType(false))
	t.Run("vote is a float", testWithWrongType(3.4))
}
