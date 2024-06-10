package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/messagedata/election/melection"
	"popstellar/internal/handler/messagedata/election/telection"
	"popstellar/internal/handler/messagedata/federation/mfederation"
	mlao2 "popstellar/internal/handler/messagedata/lao/mlao"
	generator2 "popstellar/internal/test/generator"
	"sort"
	"testing"
	"time"
)

//======================================================================================================================
// Repository interface implementation tests
//======================================================================================================================

func Test_SQLite_GetMessageByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}

	expected := []mmessage.Message{testMessages[0].msg,
		testMessages[1].msg,
		testMessages[2].msg,
		testMessages[3].msg}
	IDs := []string{"ID1", "ID2", "ID3", "ID4"}
	for i, elem := range IDs {
		msg, err := lite.GetMessageByID(elem)
		require.NoError(t, err)
		require.Equal(t, expected[i], msg)
	}
}

func Test_SQLite_GetMessagesByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}

	IDs := []string{"ID1", "ID2", "ID3", "ID4"}
	expected := map[string]mmessage.Message{"ID1": testMessages[0].msg,
		"ID2": testMessages[1].msg,
		"ID3": testMessages[2].msg,
		"ID4": testMessages[3].msg}

	messages, err := lite.GetMessagesByID(IDs)
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

//======================================================================================================================
// Helper functions
//======================================================================================================================

func newFakeSQLite(t *testing.T) (SQLite, string, error) {
	dir, err := os.MkdirTemp("", "test-")
	require.NoError(t, err)

	fn := filepath.Join(dir, "test.DB")
	lite, err := NewSQLite(fn, false)
	require.NoError(t, err)

	return lite, dir, nil
}

type testMessage struct {
	msg     mmessage.Message
	channel string
}

func newTestMessages() []testMessage {
	message1 := mmessage.Message{Data: base64.URLEncoding.EncodeToString([]byte("data1")),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	message2 := mmessage.Message{Data: base64.URLEncoding.EncodeToString([]byte("data2")),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	message3 := mmessage.Message{Data: base64.URLEncoding.EncodeToString([]byte("data3")),
		Sender:            "sender3",
		Signature:         "sig3",
		MessageID:         "ID3",
		WitnessSignatures: []mmessage.WitnessSignature{},
	}
	message4 := message3
	message4.MessageID = "ID4"

	return []testMessage{{msg: message1, channel: "channel1"},
		{msg: message2, channel: "channel2"},
		{msg: message3, channel: "channel1/subChannel1"},
		{msg: message4, channel: "channel1"},
	}
}

func Test_SQLite_GetAllMessagesFromChannel(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}

	expected := []mmessage.Message{testMessages[3].msg, testMessages[0].msg}
	messages, err := lite.GetAllMessagesFromChannel("channel1")
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

func Test_SQLite_GetChannelType(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	err = lite.StoreChannel("channel1", "root", "")
	require.NoError(t, err)

	channelType, err := lite.GetChannelType("channel1")
	require.NoError(t, err)
	require.Equal(t, "root", channelType)
}

func Test_SQLite_GetResultForGetMessagesByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}

	expected := map[string][]mmessage.Message{
		"channel1":             {testMessages[0].msg, testMessages[3].msg},
		"channel2":             {testMessages[1].msg},
		"channel1/subChannel1": {testMessages[2].msg}}
	params := map[string][]string{
		"channel1":             {"ID1", "ID4"},
		"channel2":             {"ID2"},
		"channel1/subChannel1": {"ID3"}}
	result, err := lite.GetResultForGetMessagesByID(params)
	require.NoError(t, err)
	require.Equal(t, expected, result)
}

func Test_SQLite_GetParamsForGetMessageByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}
	params := map[string][]string{
		"channel1":             {"other_ID1", "other_ID4", "ID1", "ID4"},
		"channel2":             {"other_ID2", "ID2"},
		"channel1/subChannel1": {"other_ID3", "ID3"},
		"other_channel":        {"other_ID5", "other_ID6"}}

	expected := map[string][]string{
		"channel1":             {"other_ID1", "other_ID4"},
		"channel2":             {"other_ID2"},
		"channel1/subChannel1": {"other_ID3"},
		"other_channel":        {"other_ID5", "other_ID6"}}
	result, err := lite.GetParamsForGetMessageByID(params)
	require.NoError(t, err)
	require.Equal(t, expected, result)
}

func Test_SQLite_HasChannel(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	err = lite.StoreChannel(
		"channel1",
		"root",
		"")
	require.NoError(t, err)

	ok, err := lite.HasChannel("channel1")
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.HasChannel("channel2")
	require.NoError(t, err)
	require.False(t, ok)
}

func TestSQLite_HasMessage(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	message5 := mmessage.Message{Data: base64.URLEncoding.EncodeToString([]byte("data5")),
		Sender:            "sender5",
		Signature:         "sig5",
		MessageID:         "ID5",
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	err = lite.StoreMessageAndData("channel1", message5)
	require.NoError(t, err)

	ok, err := lite.HasMessage("ID5")
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.HasMessage("ID1")
	require.NoError(t, err)
	require.False(t, ok)
}

func Test_SQLite_StoreLaoWithLaoGreet(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	channels := map[string]string{
		"laoPath":  "lao",
		"channel1": "chirp",
		"channel2": "coin",
		"channel3": "auth",
		"channel4": "consensus",
		"channel5": "reaction"}

	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	organizerPubKey := point
	organizerPubBuf, err := organizerPubKey.MarshalBinary()
	require.NoError(t, err)

	organizerPubBuf64 := base64.URLEncoding.EncodeToString(organizerPubBuf)

	laoID := "laoID"

	laoCreateMsg := generator2.NewLaoCreateMsg(t, "sender1", laoID, "laoName", 123456789,
		organizerPubBuf64, nil)

	laoGreet := mlao2.LaoGreet{
		Object:   "lao",
		Action:   "greet",
		LaoID:    laoID,
		Frontend: "frontend",
		Address:  "address",
		Peers:    []mlao2.Peer{{Address: "peer1"}, {Address: "peer2"}},
	}
	laoGreetBytes, err := json.Marshal(laoGreet)
	require.NoError(t, err)

	laoGreetMsg := mmessage.Message{Data: base64.URLEncoding.EncodeToString(laoGreetBytes),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []mmessage.WitnessSignature{}}

	err = lite.StoreLaoWithLaoGreet(channels, laoID, organizerPubBuf, laoCreateMsg, laoGreetMsg)
	require.NoError(t, err)

	expected := []mmessage.Message{laoGreetMsg, laoCreateMsg}

	sort.Slice(expected, func(i, j int) bool {
		return expected[i].MessageID < expected[j].MessageID
	})
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)

	sort.Slice(expected, func(i, j int) bool {
		return messages[i].MessageID < messages[j].MessageID
	})
	require.Equal(t, expected, messages)

	expected = []mmessage.Message{laoCreateMsg}
	messages, err = lite.GetAllMessagesFromChannel("/root")
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	for channel, expectedType := range channels {
		ok, err := lite.HasChannel(channel)
		require.NoError(t, err)
		require.True(t, ok)
		channelType, err := lite.GetChannelType(channel)
		require.NoError(t, err)
		require.Equal(t, expectedType, channelType)
	}

	returnedKey, err := lite.GetOrganizerPubKey(laoID)
	require.NoError(t, err)
	organizerPubKey.Equal(returnedKey)
	require.True(t, organizerPubKey.Equal(returnedKey))

	// Test that we can retrieve the organizer public key from the election channel
	electionPath := "electionID"
	err = lite.StoreChannel(electionPath, "election", laoID)
	require.NoError(t, err)
	returnedKey, err = lite.GetLAOOrganizerPubKey(electionPath)
	require.NoError(t, err)
	require.True(t, organizerPubKey.Equal(returnedKey))

}

func Test_SQLite_GetRollCallState(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	rollCallCreate := generator2.NewRollCallCreateMsg(t, "sender1", "name", "createID", 1, 2, 10, nil)
	rollCallOpen := generator2.NewRollCallOpenMsg(t, "sender1", "openID", "createID", 4, nil)
	rollCallClose := generator2.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)
	states := []string{"create", "open", "close"}
	messages := []mmessage.Message{rollCallCreate, rollCallOpen, rollCallClose}

	for i, msg := range messages {
		err = lite.StoreMessageAndData("channel1", msg)
		require.NoError(t, err)
		state, err := lite.GetRollCallState("channel1")
		require.NoError(t, err)
		require.Equal(t, states[i], state)
	}
}

func Test_SQLite_CheckPrevOpenOrReopenID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	rollCallOpen := generator2.NewRollCallOpenMsg(t, "sender1", "openID", "createID", 4, nil)
	rollCallReopen := generator2.NewRollCallReOpenMsg(t, "sender1", "reopenID", "closeID", 12, nil)

	err = lite.StoreMessageAndData("channel1", rollCallOpen)
	require.NoError(t, err)

	ok, err := lite.CheckPrevOpenOrReopenID("channel1", "openID")
	require.NoError(t, err)
	require.True(t, ok)

	err = lite.StoreMessageAndData("channel1", rollCallReopen)
	require.NoError(t, err)

	ok, err = lite.CheckPrevOpenOrReopenID("channel1", "reopenID")
	require.NoError(t, err)
	require.True(t, ok)
}

func Test_SQLite_CheckPrevCreateOrCloseID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	rollCallCreate := generator2.NewRollCallCreateMsg(t, "sender1", "name", "createID", 1, 2, 10, nil)
	rollCallClose := generator2.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)

	err = lite.StoreMessageAndData("channel1", rollCallCreate)
	require.NoError(t, err)

	ok, err := lite.CheckPrevCreateOrCloseID("channel1", "createID")
	require.NoError(t, err)
	require.True(t, ok)

	err = lite.StoreMessageAndData("channel1", rollCallClose)
	require.NoError(t, err)

	ok, err = lite.CheckPrevCreateOrCloseID("channel1", "closeID")
	require.NoError(t, err)
	require.True(t, ok)
}

func Test_SQLite_StoreRollCallClose(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	channels := []string{"channel1", "channel2", "channel3"}
	laoID := "laoID"

	rollCallClose := generator2.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)

	err = lite.StoreRollCallClose(channels, laoID, rollCallClose)
	require.NoError(t, err)

	expected := []mmessage.Message{rollCallClose}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	for _, channel := range channels {
		ok, err := lite.HasChannel(channel)
		require.NoError(t, err)
		require.True(t, ok)
	}
}

func Test_SQLite_StoreElectionWithElectionKey(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	laoID := "laoID"
	electionID := "electionID"
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	electionPubBuf, err := point.MarshalBinary()
	require.NoError(t, err)

	electionSetupMsg := generator2.NewElectionSetupMsg(t, "sender1", "ID1", laoID, "electionName",
		"version", 1, 2, 3, nil, nil)

	electionKey := melection.ElectionKey{
		Object:   "election",
		Action:   "key",
		Election: electionID,
		Key:      base64.URLEncoding.EncodeToString(electionPubBuf),
	}

	electionKeyBytes, err := json.Marshal(electionKey)
	require.NoError(t, err)

	electionKeyMsg := mmessage.Message{
		Data:              base64.URLEncoding.EncodeToString(electionKeyBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID2",
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	err = lite.StoreElectionWithElectionKey(laoID, electionID, point, secret, electionSetupMsg, electionKeyMsg)
	require.NoError(t, err)

	expected := []mmessage.Message{electionSetupMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []mmessage.Message{electionKeyMsg, electionSetupMsg}
	messages, err = lite.GetAllMessagesFromChannel(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	returnedSecretKey, err := lite.GetElectionSecretKey(electionID)
	require.NoError(t, err)
	require.True(t, secret.Equal(returnedSecretKey))
}

func Test_SQLite_StoreElection(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	laoID := "laoID"
	electionID := "electionID"
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	electionSetupMsg := generator2.NewElectionSetupMsg(t, "sender1", "ID1", laoID, "electionName",
		"version", 1, 2, 3, nil, nil)

	err = lite.StoreElection(laoID, electionID, point, secret, electionSetupMsg)
	require.NoError(t, err)

	expected := []mmessage.Message{electionSetupMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []mmessage.Message{electionSetupMsg}
	messages, err = lite.GetAllMessagesFromChannel(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	returnedSecretKey, err := lite.GetElectionSecretKey(electionID)
	require.NoError(t, err)
	require.True(t, secret.Equal(returnedSecretKey))
}

func Test_SQLite_IsElectionStartedOrTerminated(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionPath := "electionPath"
	electionID := "electionID"
	laoID := "laoID"
	ok, err := lite.IsElectionStartedOrEnded(electionPath)
	require.NoError(t, err)
	require.False(t, ok)

	electionOpenMsg := generator2.NewElectionOpenMsg(t, "sender1", laoID, electionID, 1, nil)

	err = lite.StoreMessageAndData(electionID, electionOpenMsg)
	require.NoError(t, err)
	ok, err = lite.IsElectionStartedOrEnded(electionID)
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.IsElectionStarted(electionID)
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.IsElectionEnded(electionID)
	require.NoError(t, err)
	require.False(t, ok)

	electionCloseMsg := generator2.NewElectionCloseMsg(t, "sender1", laoID, electionID, "", 1, nil)

	err = lite.StoreMessageAndData(electionID, electionCloseMsg)
	require.NoError(t, err)
	ok, err = lite.IsElectionStartedOrEnded(electionID)
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.IsElectionEnded(electionID)
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.IsElectionStarted(electionID)
	require.NoError(t, err)
	require.False(t, ok)
}

func Test_SQLite_GetElectionCreationTimeAndType(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	laoPath := "laoPath"
	electionPath := "electionPath"
	creationTime := int64(123456789)

	electionSetupMsg := generator2.NewElectionSetupMsg(t, "sender1", "ID1", laoPath, "electionName",
		melection.OpenBallot, creationTime, 2, 3, nil, nil)

	err = lite.StoreMessageAndData(electionPath, electionSetupMsg)
	require.NoError(t, err)

	returnedTime, err := lite.GetElectionCreationTime(electionPath)
	require.NoError(t, err)
	require.Equal(t, creationTime, returnedTime)

	electionType, err := lite.GetElectionType(electionPath)
	require.NoError(t, err)
	require.Equal(t, melection.OpenBallot, electionType)
}

func Test_SQLite_GetElectionAttendees(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionID := "electionID"
	laoID := "laoID"
	attendees := []string{"attendee1", "attendee2", "attendee3"}
	expected := map[string]struct{}{"attendee1": {}, "attendee2": {}, "attendee3": {}}

	rollCallCloseMsg := generator2.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, attendees, nil)

	err = lite.StoreMessageAndData(laoID, rollCallCloseMsg)
	require.NoError(t, err)

	err = lite.StoreChannel(electionID, "election", laoID)
	require.NoError(t, err)

	returnedAttendees, err := lite.GetElectionAttendees(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, returnedAttendees)
}

func Test_SQLite_GetElectionQuestionsWithVotes(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionPath := "electionPath"
	laoPath := "laoPath"
	laoID := "laoID"
	electionID := "electionID"
	questions := []melection.ElectionSetupQuestion{
		{
			ID:            "questionID1",
			Question:      "question1",
			VotingMethod:  "Plurality",
			BallotOptions: []string{"Option1", "Option2"},
		},
	}

	electionSetupMsg := generator2.NewElectionSetupMsg(t, "sender1", "ID1", laoPath, "electionName",
		melection.OpenBallot, 1, 2, 3, questions, nil)

	err = lite.StoreMessageAndData(electionPath, electionSetupMsg)
	require.NoError(t, err)

	data64, err := base64.URLEncoding.DecodeString(electionSetupMsg.Data)
	require.NoError(t, err)

	var electionSetup melection.ElectionSetup
	err = json.Unmarshal(data64, &electionSetup)
	require.NoError(t, err)

	expected, err := getQuestionsFromMessage(electionSetup)
	require.NoError(t, err)

	// Add votes to the election
	vote1 := generator2.VoteString{ID: "voteID1", Question: "questionID1", Vote: "Option1"}
	votes := []generator2.VoteString{vote1}
	castVoteMsg := generator2.NewVoteCastVoteStringMsg(t, "sender1", laoID, electionID,
		1, votes, nil)

	err = lite.StoreMessageAndData(electionPath, castVoteMsg)
	require.NoError(t, err)

	question1 := expected["questionID1"]
	question1.ValidVotes = map[string]telection.ValidVote{
		"sender1": {MsgID: castVoteMsg.MessageID, ID: "voteID1", VoteTime: 1, Index: "Option1"},
	}
	expected["questionID1"] = question1

	result, err := lite.GetElectionQuestionsWithValidVotes(electionPath)
	require.NoError(t, err)
	require.Equal(t, expected, result)

	// Add more votes to the election
	vote2 := generator2.VoteString{ID: "voteID2", Question: "questionID1", Vote: "Option2"}
	votes = []generator2.VoteString{vote2}
	castVoteMsg = generator2.NewVoteCastVoteStringMsg(t, "sender1", laoID, electionID,
		2, votes, nil)

	err = lite.StoreMessageAndData(electionPath, castVoteMsg)
	require.NoError(t, err)

	question1 = expected["questionID1"]
	question1.ValidVotes = map[string]telection.ValidVote{
		"sender1": {MsgID: castVoteMsg.MessageID, ID: "voteID2", VoteTime: 2, Index: "Option2"},
	}
	expected["questionID1"] = question1

	result, err = lite.GetElectionQuestionsWithValidVotes(electionPath)
	require.NoError(t, err)
	require.Equal(t, expected, result)
}

func Test_SQLite_StoreElectionEndWithResult(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionPath := "electionPath"
	laoID := "laoID"
	electionID := "electionID"

	electionEndMsg := generator2.NewElectionCloseMsg(t, "sender1", laoID, electionID, "", 1, nil)
	electionResultMsg := generator2.NewElectionResultMsg(t, "sender2", nil, nil)

	err = lite.StoreElectionEndWithResult(electionPath, electionEndMsg, electionResultMsg)
	require.NoError(t, err)

	expected := []mmessage.Message{electionEndMsg, electionResultMsg}
	messages, err := lite.GetAllMessagesFromChannel(electionPath)
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

func Test_SQLite_StoreChirpMessages(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	chirpPath := "chirpID"
	generalChirpPath := "generalChirpID"

	chirpMsg := generator2.NewChirpAddMsg(t, "sender1", nil, 1)
	generalChirpMsg := mmessage.Message{
		Data:      base64.URLEncoding.EncodeToString([]byte("data")),
		Sender:    "sender1",
		Signature: "sig2",
		MessageID: "ID2",
	}

	err = lite.StoreChirpMessages(chirpPath, generalChirpPath, chirpMsg, generalChirpMsg)
	require.NoError(t, err)

	expected := []mmessage.Message{chirpMsg}
	messages, err := lite.GetAllMessagesFromChannel(chirpPath)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []mmessage.Message{generalChirpMsg}
	messages, err = lite.GetAllMessagesFromChannel(generalChirpPath)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

}

func Test_SQLite_IsAttendee(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	attendees := []string{"attendee1", "attendee2", "attendee3"}
	laoID := "laoID"

	rollCallCloseMsg := generator2.NewRollCallCloseMsg(t, "sender1", "closeID", "openID",
		8, attendees, nil)

	err = lite.StoreMessageAndData(laoID, rollCallCloseMsg)
	require.NoError(t, err)

	ok, err := lite.IsAttendee(laoID, "attendee1")
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.IsAttendee(laoID, "attendee4")
	require.NoError(t, err)
	require.False(t, ok)
}

func Test_SQLite_GetReactionSender(t *testing.T) {

	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	reactionAddMsg := generator2.NewReactionAddMsg(t, "sender1", nil, "", "chirpID", 1)

	sender, err := lite.GetReactionSender(reactionAddMsg.MessageID)
	require.NoError(t, err)
	require.Equal(t, "", sender)

	err = lite.StoreMessageAndData("channel1", reactionAddMsg)
	require.NoError(t, err)
	sender, err = lite.GetReactionSender(reactionAddMsg.MessageID)
	require.NoError(t, err)
	require.Equal(t, "sender1", sender)
}

func Test_SQLite_IsChallengeValid(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	sender := "qlDMnnFv_W4zovvD0pp4FFjCfJ78z_O7LqxBXGdO0lA="
	notSender := "7_9A6K6dbfN04GUwEaLCDNLcdnaTjmBVw1qWH_C9M3s="
	fedPath := "/root/lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA=/federation"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	challenge := mfederation.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator2.NewFederationChallenge(t, sender, value,
		validUntil, nil)

	err = lite.StoreMessageAndData(fedPath, challengeMsg)
	require.NoError(t, err)

	err = lite.IsChallengeValid(sender, challenge, fedPath)
	require.NoError(t, err)

	err = lite.IsChallengeValid(notSender, challenge, fedPath)
	require.Error(t, err, sql.ErrNoRows)

	challenge.Value = "12345678"
	err = lite.IsChallengeValid(sender, challenge, fedPath)
	require.Error(t, err, sql.ErrNoRows)

	challenge.Value = value
	challenge.ValidUntil = validUntil + 1
	err = lite.IsChallengeValid(sender, challenge, fedPath)
	require.Error(t, err, sql.ErrNoRows)
}

func Test_SQLite_GetFederationExpect(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	organizer := "qlDMnnFv_W4zovvD0pp4FFjCfJ78z_O7LqxBXGdO0lA="
	organizer2 := "NzfRC3bUGHLy-I_iUbDXq9CAXcnnDdue9P2hqJHF6bk="

	laoId := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	fedPath := fmt.Sprintf("/root/%s/federation", laoId)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	challenge := mfederation.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator2.NewFederationChallenge(t, organizer, value,
		validUntil, nil)

	expectMsg := generator2.NewFederationExpect(t, organizer, laoId,
		serverAddressA, organizer2, challengeMsg, nil)

	_, err = lite.GetFederationExpect(organizer, organizer2, challenge, fedPath)
	require.ErrorIs(t, err, sql.ErrNoRows)

	err = lite.StoreMessageAndData(fedPath, expectMsg)
	require.NoError(t, err)

	expect, err := lite.GetFederationExpect(organizer, organizer2, challenge, fedPath)
	require.NoError(t, err)
	require.Equal(t, challengeMsg, expect.ChallengeMsg)
	require.Equal(t, organizer2, expect.PublicKey)
	require.Equal(t, serverAddressA, expect.ServerAddress)
	require.Equal(t, laoId, expect.LaoId)

	_, err = lite.GetFederationExpect(organizer2, organizer, challenge, fedPath)
	require.ErrorIs(t, err, sql.ErrNoRows)
}

func Test_SQLite_GetFederationInit(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	organizer := "qlDMnnFv_W4zovvD0pp4FFjCfJ78z_O7LqxBXGdO0lA="
	organizer2 := "NzfRC3bUGHLy-I_iUbDXq9CAXcnnDdue9P2hqJHF6bk="

	laoId := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	fedPath := fmt.Sprintf("/root/%s/federation", laoId)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	challenge := mfederation.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator2.NewFederationChallenge(t, organizer, value,
		validUntil, nil)

	expectMsg := generator2.NewFederationInit(t, organizer, laoId,
		serverAddressA, organizer2, challengeMsg, nil)

	_, err = lite.GetFederationInit(organizer, organizer2, challenge, fedPath)
	require.ErrorIs(t, err, sql.ErrNoRows)

	err = lite.StoreMessageAndData(fedPath, expectMsg)
	require.NoError(t, err)

	expect, err := lite.GetFederationInit(organizer, organizer2, challenge, fedPath)
	require.NoError(t, err)
	require.Equal(t, challengeMsg, expect.ChallengeMsg)
	require.Equal(t, organizer2, expect.PublicKey)
	require.Equal(t, serverAddressA, expect.ServerAddress)
	require.Equal(t, laoId, expect.LaoId)

	_, err = lite.GetFederationInit(organizer2, organizer, challenge, fedPath)
	require.ErrorIs(t, err, sql.ErrNoRows)
}
