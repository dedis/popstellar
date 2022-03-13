package election

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestVerify_CastVote(t *testing.T) {
	// create the election channel
	electChannel, _ := newFakeChannel(t)
	electChannel.started = true

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote",
		"vote_cast_vote.json"))
	require.NoError(t, err)

	// object and action
	object, action := "election", "cast_vote"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var castVote messagedata.VoteCastVote

	err = json.Unmarshal(buf, &castVote)
	require.NoError(t, err)

	// test valid example
	err = electChannel.verifyMessageCastVote(castVote)
	require.NoError(t, err)

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err = os.ReadFile(filepath.Join(relativeMsgDataExamplePath,
				"vote_cast_vote", file))
			require.NoError(t, err)

			obj, act, err = messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &castVote)
			require.NoError(t, err)

			err = electChannel.verifyMessageCastVote(castVote)
			require.Error(t, err)
		}
	}

	t.Run("lao id not base64", getTestBadExample("bad_vote_cast_vote_lao_not_base64.json"))
	t.Run("election id noy base64", getTestBadExample("bad_vote_cast_vote_election_not_base64.json"))
	t.Run("lao id invalid hash", getTestBadExample("bad_vote_cast_vote_lao_invalid_hash.json"))
	t.Run("election id invalid hash", getTestBadExample("bad_vote_cast_vote_election_invalid_hash.json"))
	t.Run("created at negative", getTestBadExample("bad_vote_cast_vote_created_at_negative.json"))
	t.Run("created at before start", getTestBadExample("bad_vote_cast_vote_created_at_before_start.json"))
	t.Run("created at after end", getTestBadExample("bad_vote_cast_vote_created_at_after_end.json"))
}

func TestVerify_ElectionEnd(t *testing.T) {
	// create the election channel
	electChannel, _ := newFakeChannel(t)
	electChannel.started = true

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end",
		"election_end.json"))
	require.NoError(t, err)

	// object and action
	object, action := "election", "end"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionEnd messagedata.ElectionEnd

	err = json.Unmarshal(buf, &electionEnd)
	require.NoError(t, err)

	// test valid example
	err = electChannel.verifyMessageElectionEnd(electionEnd)
	require.NoError(t, err)

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err = os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end", file))
			require.NoError(t, err)

			obj, act, err = messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &electionEnd)
			require.NoError(t, err)

			err = electChannel.verifyMessageElectionEnd(electionEnd)
			require.Error(t, err)
		}
	}

	t.Run("lao id not base64", getTestBadExample("bad_election_end_lao_not_base64.json"))
	t.Run("election id not base64", getTestBadExample("bad_election_end_election_not_base64.json"))
	t.Run("lao id invalid hash", getTestBadExample("bad_election_end_lao_invalid_hash.json"))
	t.Run("election id invalid hash", getTestBadExample("bad_election_end_election_invalid_hash.json"))
	t.Run("created at negative", getTestBadExample("bad_election_end_created_at_negative.json"))
	t.Run("created at before election end", getTestBadExample("bad_election_end_created_at_before_end.json"))
	t.Run("registered votes not base64", getTestBadExample("bad_election_end_registered_votes_not_base64.json"))
}
