package election

import (
	"encoding/base64"
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	melection2 "popstellar/internal/handler/messagedata/election/melection"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestVerify_ElectionOpen(t *testing.T) {
	// create the election channel
	electChannel, _ := newFakeChannel(t, false)

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_open",
		"election_open.json"))
	require.NoError(t, err)

	object, action := "election", "open"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionOpen melection2.ElectionOpen

	err = json.Unmarshal(buf, &electionOpen)
	require.NoError(t, err)

	// test valid example
	err = electChannel.verifyMessageElectionOpen(electionOpen)
	require.NoError(t, err)

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err = os.ReadFile(filepath.Join(relativeMsgDataExamplePath,
				"election_open", file))
			require.NoError(t, err)

			obj, act, err = messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &electionOpen)
			require.NoError(t, err)

			err = electChannel.verifyMessageElectionOpen(electionOpen)
			require.Error(t, err)
		}
	}

	t.Run("lao id not base64", getTestBadExample("bad_election_open_lao_not_base64.json"))
	t.Run("election id not base64", getTestBadExample("bad_election_open_election_not_base64.json"))
	t.Run("lao id invalid hash", getTestBadExample("bad_election_open_lao_invalid_hash.json"))
	t.Run("election id invalid hash", getTestBadExample("bad_election_open_election_invalid_hash.json"))
	t.Run("opened at negative", getTestBadExample("bad_election_open_opened_at_negative.json"))
}

func TestVerify_ElectionOpen_already_open(t *testing.T) {
	// create the opened election channel
	electChannel, _ := newFakeChannel(t, false)
	electChannel.started = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_open",
		"election_open.json"))
	require.NoError(t, err)

	var electionOpen melection2.ElectionOpen

	err = json.Unmarshal(buf, &electionOpen)
	require.NoError(t, err)

	// send the election open message to the channel
	err = electChannel.verifyMessageElectionOpen(electionOpen)
	require.Error(t, err)
}

func TestVerify_ElectionOpen_already_closed(t *testing.T) {
	// create the terminated election channel
	electChannel, _ := newFakeChannel(t, false)
	electChannel.terminated = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_open",
		"election_open.json"))
	require.NoError(t, err)

	var electionOpen melection2.ElectionOpen

	err = json.Unmarshal(buf, &electionOpen)
	require.NoError(t, err)

	// send the election open message to the channel
	err = electChannel.verifyMessageElectionOpen(electionOpen)
	require.Error(t, err)
}

func TestVerify_ElectionOpen_Created_Time_Less_Than_Create_Time_Setup(t *testing.T) {
	// create the opened election channel with election open time less than
	// election creation time
	electChannel, _ := newFakeChannel(t, false)

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_open",
		"election_open.json"))
	require.NoError(t, err)

	var electionOpen melection2.ElectionOpen

	err = json.Unmarshal(buf, &electionOpen)
	require.NoError(t, err)

	electChannel.createdAt = electionOpen.OpenedAt + 100

	// send the election open message to the channel
	err = electChannel.verifyMessageElectionOpen(electionOpen)
	require.Error(t, err)
}

func TestVerify_ElectionEnd_Created_Time_Less_Than_Create_Time_Setup(t *testing.T) {
	// create the opened election channel with election open time less than
	// election creation time
	electChannel, _ := newFakeChannel(t, false)
	electChannel.started = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end",
		"election_end.json"))
	require.NoError(t, err)

	var electionEnd melection2.ElectionEnd

	err = json.Unmarshal(buf, &electionEnd)
	require.NoError(t, err)

	electChannel.createdAt = electionEnd.CreatedAt + 100

	// send the election open message to the channel
	err = electChannel.verifyMessageElectionEnd(electionEnd)
	require.Error(t, err)
	require.Contains(t, err.Error(), "election end cannot have a creation time prior to election setup ")
}

func TestVerify_CastVote_Created_Time_Less_Than_Create_Time_Setup(t *testing.T) {
	// create the opened election channel with election open time less than
	// election creation time
	electChannel, _ := newFakeChannel(t, false)
	electChannel.started = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote",
		"vote_cast_vote.json"))
	require.NoError(t, err)

	var castVote melection2.VoteCastVote

	err = json.Unmarshal(buf, &castVote)
	require.NoError(t, err)

	electChannel.createdAt = castVote.CreatedAt + 100

	// send the election open message to the channel
	err = electChannel.verifyMessageCastVote(castVote)
	require.Error(t, err)
	require.Contains(t, err.Error(), "cast vote cannot have a creation time prior to election setup")
}

func TestVerify_CastVote_Open_Ballot(t *testing.T) {
	// create the election channel
	electChannel, _ := newFakeChannel(t, false)
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

	var castVote melection2.VoteCastVote

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

	t.Run("lao id not base64", getTestBadExample("wrong_vote_cast_vote_lao_not_base64.json"))
	t.Run("election id not base64", getTestBadExample("wrong_vote_cast_vote_election_not_base64.json"))
	t.Run("lao id invalid hash", getTestBadExample("wrong_vote_cast_vote_lao_invalid_hash.json"))
	t.Run("election id invalid hash", getTestBadExample("wrong_vote_cast_vote_election_invalid_hash.json"))
	t.Run("created at negative", getTestBadExample("wrong_vote_cast_vote_created_at_negative.json"))
	t.Run("vote is encrypted", getTestBadExample("vote_cast_vote_encrypted.json"))
}

func TestVerify_CastVote_Secret_Ballot(t *testing.T) {
	// create the election channel
	electChannel, _ := newFakeChannel(t, true)
	electChannel.started = true

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote",
		"vote_cast_vote_encrypted.json"))
	require.NoError(t, err)

	// object and action
	object, action := "election", "cast_vote"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var castVote melection2.VoteCastVote

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

	t.Run("vote is unencrypted", getTestBadExample("vote_cast_vote.json"))
	t.Run("vote is not base64", getTestBadExample("wrong_vote_cast_vote_encrypted_vote_not_base64.json"))
	t.Run("vote is not 64 bytes long", getTestBadExample("wrong_vote_cast_vote_not_right_length.json"))
}

func TestVerify_CastVote_not_open(t *testing.T) {
	// create the non opened election channel
	electChannel, _ := newFakeChannel(t, false)

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote",
		"vote_cast_vote.json"))
	require.NoError(t, err)

	var voteCastVote melection2.VoteCastVote

	err = json.Unmarshal(buf, &voteCastVote)
	require.NoError(t, err)

	// send the cast vote message to the channel
	err = electChannel.verifyMessageCastVote(voteCastVote)
	require.Error(t, err)
}

func TestVerify_CastVote_already_closed(t *testing.T) {
	// create the terminated election channel
	electChannel, _ := newFakeChannel(t, false)
	electChannel.terminated = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote",
		"vote_cast_vote.json"))
	require.NoError(t, err)

	var voteCastVote melection2.VoteCastVote

	err = json.Unmarshal(buf, &voteCastVote)
	require.NoError(t, err)

	// send the cast vote message to the channel
	err = electChannel.verifyMessageCastVote(voteCastVote)
	require.Error(t, err)
}

func TestVerify_ElectionEnd(t *testing.T) {
	// create the election channel
	electChannel, pkOrganizer := newFakeChannel(t, false)
	electChannel.started = true

	// Cast a vote for the election
	file := filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote", "vote_cast_vote.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var castVote melection2.VoteCastVote
	err = json.Unmarshal(buf, &castVote)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	// wrap the cast vote in a message
	m := mmessage.Message{
		Data:              buf64,
		Sender:            pkOrganizer,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	err = electChannel.processCastVote(m, &castVote, &fakeSocket{})
	require.NoError(t, err)

	// read the valid example file
	buf, err = os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end",
		"election_end.json"))
	require.NoError(t, err)

	// object and action
	object, action := "election", "end"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionEnd melection2.ElectionEnd

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
	t.Run("registered votes not base64", getTestBadExample("bad_election_end_registered_votes_not_base64.json"))
}

func TestVerify_ElectionEnd_not_open(t *testing.T) {
	// create the non opened election channel
	electChannel, _ := newFakeChannel(t, false)

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end",
		"election_end.json"))
	require.NoError(t, err)

	var electionEnd melection2.ElectionEnd

	err = json.Unmarshal(buf, &electionEnd)
	require.NoError(t, err)

	// send election end to the channel
	err = electChannel.verifyMessageElectionEnd(electionEnd)
	require.Error(t, err)
}

func TestVerifyRegisteredVotes_Badly_Sorted(t *testing.T) {
	questions := map[string]*question{
		"question1": {
			validVotes: map[string]validVote{
				"vote1.2": {
					msgID: "aa",
					ID:    "vote1.2",
				},
				"vote1.1": {
					msgID: "bb",
					ID:    "vote1.1",
				},
			},
		},
		"question2": {
			validVotes: map[string]validVote{
				"vote2.1": {
					msgID: "bb",
					ID:    "vote2.1",
				},
				"vote2.2": {
					msgID: "aa",
					ID:    "vote2.2",
				},
			},
		},
	}

	// votes must be sorted by by vote id
	expected := messagedata.Hash("vote1.1", "vote2.1", "vote2.2", "vote1.2")

	end := melection2.ElectionEnd{
		RegisteredVotes: expected,
	}

	err := verifyRegisteredVotes(end, &questions)
	require.Error(t, err)
}

func TestVerifyRegisteredVotes_OK(t *testing.T) {
	questions := map[string]*question{
		"question1": {
			validVotes: map[string]validVote{
				"vote1.2": {
					msgID: "aa",
					ID:    "vote1.2",
				},
				"vote1.1": {
					msgID: "bb",
					ID:    "vote1.1",
				},
			},
		},
		"question2": {
			validVotes: map[string]validVote{
				"vote2.1": {
					msgID: "bb",
					ID:    "vote2.1",
				},
				"vote2.2": {
					msgID: "aa",
					ID:    "vote2.2",
				},
			},
		},
	}

	// votes must be sorted by by vote id
	expected := messagedata.Hash("vote1.1", "vote1.2", "vote2.1", "vote2.2")

	end := melection2.ElectionEnd{
		RegisteredVotes: expected,
	}

	err := verifyRegisteredVotes(end, &questions)
	require.NoError(t, err)
}

func TestVerify_ElectionEnd_already_closed(t *testing.T) {
	// create the terminated election channel
	electChannel, _ := newFakeChannel(t, false)
	electChannel.terminated = true

	buf, err := os.ReadFile(filepath.Join(relativeMsgDataExamplePath, "election_end",
		"election_end.json"))
	require.NoError(t, err)

	var electionEnd melection2.ElectionEnd

	err = json.Unmarshal(buf, &electionEnd)
	require.NoError(t, err)

	// send election end to the channel
	err = electChannel.verifyMessageElectionEnd(electionEnd)
	require.Error(t, err)
}

func Test_Array_To_String(t *testing.T) {
	a := []int{0, 1, 3, 5, 7, 9, 0}

	require.Equal(t, "0,1,3,5,7,9,0", arrayToString(a, ","))
	require.Equal(t, "0.1.3.5.7.9.0", arrayToString(a, "."))
}
