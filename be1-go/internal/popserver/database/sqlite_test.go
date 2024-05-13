package database

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/crypto"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"sort"
	"testing"
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

	expected := []message.Message{testMessages[0].msg,
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
	expected := map[string]message.Message{"ID1": testMessages[0].msg,
		"ID2": testMessages[1].msg,
		"ID3": testMessages[2].msg,
		"ID4": testMessages[3].msg}

	messages, err := lite.GetMessagesByID(IDs)
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

func Test_SQLite_AddWitnessSignature(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := newTestMessages()
	for _, m := range testMessages {
		err = lite.StoreMessageAndData(m.channel, m.msg)
		require.NoError(t, err)
	}
	// Add signatures to message1
	expected := []message.WitnessSignature{{Witness: "witness1", Signature: "sig1"}, {Witness: "witness2", Signature: "sig2"}}
	err = lite.AddWitnessSignature("ID1", "witness1", "sig1")
	require.NoError(t, err)
	err = lite.AddWitnessSignature("ID1", "witness2", "sig2")
	require.NoError(t, err)

	//Verify that the signature have been added to the message
	msg1, err := lite.GetMessageByID("ID1")
	require.NoError(t, err)
	require.Equal(t, expected, msg1.WitnessSignatures)

	message4 := message.Message{Data: base64.URLEncoding.EncodeToString([]byte("data4")),
		Sender:            "sender4",
		Signature:         "sig4",
		MessageID:         "ID5",
		WitnessSignatures: []message.WitnessSignature{},
	}

	// Add signatures to message4 who is not currently stored
	err = lite.AddWitnessSignature("ID5", "witness2", "sig3")
	require.NoError(t, err)

	//Verify that the signature has been added to the message
	err = lite.StoreMessageAndData("channel1", message4)
	require.NoError(t, err)
	expected = []message.WitnessSignature{{Witness: "witness2", Signature: "sig3"}}
	msg4, err := lite.GetMessageByID("ID5")
	require.NoError(t, err)
	require.Equal(t, expected, msg4.WitnessSignatures)
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
	msg     message.Message
	channel string
}

func newTestMessages() []testMessage {
	message1 := message.Message{Data: base64.URLEncoding.EncodeToString([]byte("data1")),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message2 := message.Message{Data: base64.URLEncoding.EncodeToString([]byte("data2")),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message3 := message.Message{Data: base64.URLEncoding.EncodeToString([]byte("data3")),
		Sender:            "sender3",
		Signature:         "sig3",
		MessageID:         "ID3",
		WitnessSignatures: []message.WitnessSignature{},
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

	expected := []message.Message{testMessages[3].msg, testMessages[0].msg}
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

	expected := map[string][]message.Message{
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

	message5 := message.Message{Data: base64.URLEncoding.EncodeToString([]byte("data5")),
		Sender:            "sender5",
		Signature:         "sig5",
		MessageID:         "ID5",
		WitnessSignatures: []message.WitnessSignature{},
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

	laoCreateMsg := generator.NewLaoCreateMsg(t, "sender1", laoID, "laoName", 123456789,
		organizerPubBuf64, nil)

	laoGreet := messagedata.LaoGreet{
		Object:   "lao",
		Action:   "greet",
		LaoID:    laoID,
		Frontend: "frontend",
		Address:  "address",
		Peers:    []messagedata.Peer{{Address: "peer1"}, {Address: "peer2"}},
	}
	laoGreetBytes, err := json.Marshal(laoGreet)
	require.NoError(t, err)

	laoGreetMsg := message.Message{Data: base64.URLEncoding.EncodeToString(laoGreetBytes),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{}}

	err = lite.StoreLaoWithLaoGreet(channels, laoID, organizerPubBuf, laoCreateMsg, laoGreetMsg)
	require.NoError(t, err)

	expected := []message.Message{laoGreetMsg, laoCreateMsg}

	sort.Slice(expected, func(i, j int) bool {
		return expected[i].MessageID < expected[j].MessageID
	})
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)

	sort.Slice(expected, func(i, j int) bool {
		return messages[i].MessageID < messages[j].MessageID
	})
	require.Equal(t, expected, messages)

	expected = []message.Message{laoCreateMsg}
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

	rollCallCreate := generator.NewRollCallCreateMsg(t, "sender1", "name", "createID", 1, 2, 10, nil)
	rollCallOpen := generator.NewRollCallOpenMsg(t, "sender1", "openID", "createID", 4, nil)
	rollCallClose := generator.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)
	states := []string{"create", "open", "close"}
	messages := []message.Message{rollCallCreate, rollCallOpen, rollCallClose}

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

	rollCallOpen := generator.NewRollCallOpenMsg(t, "sender1", "openID", "createID", 4, nil)
	rollCallReopen := generator.NewRollCallReOpenMsg(t, "sender1", "reopenID", "closeID", 12, nil)

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

	rollCallCreate := generator.NewRollCallCreateMsg(t, "sender1", "name", "createID", 1, 2, 10, nil)
	rollCallClose := generator.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)

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

	rollCallClose := generator.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, nil, nil)

	err = lite.StoreRollCallClose(channels, laoID, rollCallClose)
	require.NoError(t, err)

	expected := []message.Message{rollCallClose}
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

	electionSetupMsg := generator.NewElectionSetupMsg(t, "sender1", "ID1", laoID, "electionName",
		"version", 1, 2, 3, nil, nil)

	electionKey := messagedata.ElectionKey{
		Object:   "election",
		Action:   "key",
		Election: electionID,
		Key:      base64.URLEncoding.EncodeToString(electionPubBuf),
	}

	electionKeyBytes, err := json.Marshal(electionKey)
	require.NoError(t, err)

	electionKeyMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(electionKeyBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = lite.StoreElectionWithElectionKey(laoID, electionID, point, secret, electionSetupMsg, electionKeyMsg)
	require.NoError(t, err)

	expected := []message.Message{electionSetupMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{electionKeyMsg, electionSetupMsg}
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

	electionSetupMsg := generator.NewElectionSetupMsg(t, "sender1", "ID1", laoID, "electionName",
		"version", 1, 2, 3, nil, nil)

	err = lite.StoreElection(laoID, electionID, point, secret, electionSetupMsg)
	require.NoError(t, err)

	expected := []message.Message{electionSetupMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{electionSetupMsg}
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

	electionOpenMsg := generator.NewElectionOpenMsg(t, "sender1", laoID, electionID, 1, nil)

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

	electionCloseMsg := generator.NewElectionCloseMsg(t, "sender1", laoID, electionID, "", 1, nil)

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

	electionSetupMsg := generator.NewElectionSetupMsg(t, "sender1", "ID1", laoPath, "electionName",
		messagedata.OpenBallot, creationTime, 2, 3, nil, nil)

	err = lite.StoreMessageAndData(electionPath, electionSetupMsg)
	require.NoError(t, err)

	returnedTime, err := lite.GetElectionCreationTime(electionPath)
	require.NoError(t, err)
	require.Equal(t, creationTime, returnedTime)

	electionType, err := lite.GetElectionType(electionPath)
	require.NoError(t, err)
	require.Equal(t, messagedata.OpenBallot, electionType)
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

	rollCallCloseMsg := generator.NewRollCallCloseMsg(t, "sender1", "closeID", "openID", 8, attendees, nil)

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
	questions := []messagedata.ElectionSetupQuestion{
		{
			ID:            "questionID1",
			Question:      "question1",
			VotingMethod:  "Plurality",
			BallotOptions: []string{"Option1", "Option2"},
		},
	}

	electionSetupMsg := generator.NewElectionSetupMsg(t, "sender1", "ID1", laoPath, "electionName",
		messagedata.OpenBallot, 1, 2, 3, questions, nil)

	err = lite.StoreMessageAndData(electionPath, electionSetupMsg)
	require.NoError(t, err)

	data64, err := base64.URLEncoding.DecodeString(electionSetupMsg.Data)
	require.NoError(t, err)

	var electionSetup messagedata.ElectionSetup
	err = json.Unmarshal(data64, &electionSetup)
	require.NoError(t, err)

	expected, err := getQuestionsFromMessage(electionSetup)
	require.NoError(t, err)

	// Add votes to the election
	vote1 := generator.VoteString{ID: "voteID1", Question: "questionID1", Vote: "Option1"}
	votes := []generator.VoteString{vote1}
	castVoteMsg := generator.NewVoteCastVoteStringMsg(t, "sender1", laoID, electionID,
		1, votes, nil)

	err = lite.StoreMessageAndData(electionPath, castVoteMsg)
	require.NoError(t, err)

	question1 := expected["questionID1"]
	question1.ValidVotes = map[string]types.ValidVote{
		"sender1": {MsgID: castVoteMsg.MessageID, ID: "voteID1", VoteTime: 1, Index: "Option1"},
	}
	expected["questionID1"] = question1

	result, err := lite.GetElectionQuestionsWithValidVotes(electionPath)
	require.NoError(t, err)
	require.Equal(t, expected, result)

	// Add more votes to the election
	vote2 := generator.VoteString{ID: "voteID2", Question: "questionID1", Vote: "Option2"}
	votes = []generator.VoteString{vote2}
	castVoteMsg = generator.NewVoteCastVoteStringMsg(t, "sender1", laoID, electionID,
		2, votes, nil)

	err = lite.StoreMessageAndData(electionPath, castVoteMsg)
	require.NoError(t, err)

	question1 = expected["questionID1"]
	question1.ValidVotes = map[string]types.ValidVote{
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

	electionEndMsg := generator.NewElectionCloseMsg(t, "sender1", laoID, electionID, "", 1, nil)
	electionResultMsg := generator.NewElectionResultMsg(t, "sender2", nil, nil)

	err = lite.StoreElectionEndWithResult(electionPath, electionEndMsg, electionResultMsg)
	require.NoError(t, err)

	expected := []message.Message{electionEndMsg, electionResultMsg}
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

	chirpMsg := generator.NewChirpAddMsg(t, "sender1", nil, 1)
	generalChirpMsg := message.Message{
		Data:      base64.URLEncoding.EncodeToString([]byte("data")),
		Sender:    "sender1",
		Signature: "sig2",
		MessageID: "ID2",
	}

	err = lite.StoreChirpMessages(chirpPath, generalChirpPath, chirpMsg, generalChirpMsg)
	require.NoError(t, err)

	expected := []message.Message{chirpMsg}
	messages, err := lite.GetAllMessagesFromChannel(chirpPath)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{generalChirpMsg}
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

	rollCallCloseMsg := generator.NewRollCallCloseMsg(t, "sender1", "closeID", "openID",
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

	reactionAddMsg := generator.NewReactionAddMsg(t, "sender1", nil, "", "chirpID", 1)

	sender, err := lite.GetReactionSender(reactionAddMsg.MessageID)
	require.NoError(t, err)
	require.Equal(t, "", sender)

	err = lite.StoreMessageAndData("channel1", reactionAddMsg)
	require.NoError(t, err)
	sender, err = lite.GetReactionSender(reactionAddMsg.MessageID)
	require.NoError(t, err)
	require.Equal(t, "sender1", sender)
}
