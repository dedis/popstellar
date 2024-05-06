package database

import (
	"bufio"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/crypto"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
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

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

	message5 := message.Message{Data: "data4",
		Sender:            "sender4",
		Signature:         "sig4",
		MessageID:         "ID5",
		WitnessSignatures: []message.WitnessSignature{},
	}

	// Add signatures to message4 who is not currently stored
	err = lite.AddWitnessSignature("ID5", "witness2", "sig3")
	require.NoError(t, err)

	//Verify that the signature has been added to the message
	err = lite.StoreMessage("channel1", message5)
	require.NoError(t, err)
	expected = []message.WitnessSignature{{Witness: "witness2", Signature: "sig3"}}
	msg4, err := lite.GetMessageByID("ID5")
	require.NoError(t, err)
	require.Equal(t, expected, msg4.WitnessSignatures)
}

//======================================================================================================================
// QueryRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_GetAllMessagesFromChannel(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
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

//======================================================================================================================
// ChannelRepository interface implementation tests
//======================================================================================================================

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

	message5 := message.Message{Data: "data5",
		Sender:            "sender5",
		Signature:         "sig5",
		MessageID:         "ID5",
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = lite.StoreMessage("channel1", message5)
	require.NoError(t, err)

	ok, err := lite.HasMessage("ID5")
	require.NoError(t, err)
	require.True(t, ok)

	ok, err = lite.HasMessage("ID1")
	require.NoError(t, err)
	require.False(t, ok)
}

//======================================================================================================================
// RootRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_StoreChannelsAndMessageWithLaoGreet(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	channels := map[string]string{
		"channel1": "chirp",
		"channel2": "coin",
		"channel3": "auth",
		"channel4": "consensus",
		"channel5": "reaction"}
	laoID := "laoID"
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	organizerPubKey := point
	organizerPubBuf, err := organizerPubKey.MarshalBinary()
	require.NoError(t, err)

	laoCreate := messagedata.LaoCreate{
		Object:    "lao",
		Action:    "create",
		ID:        laoID,
		Name:      "laoName",
		Creation:  123456789,
		Organizer: base64.URLEncoding.EncodeToString(organizerPubBuf),
	}
	laoCreateBytes, err := json.Marshal(laoCreate)
	require.NoError(t, err)

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

	laoCreateMSg := message.Message{Data: base64.URLEncoding.EncodeToString(laoCreateBytes),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{}}

	laoGreetMsg := message.Message{Data: base64.URLEncoding.EncodeToString(laoGreetBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{}}

	err = lite.StoreChannelsAndMessageWithLaoGreet(channels, laoID, organizerPubBuf, laoCreateMSg, laoGreetMsg)
	require.NoError(t, err)

	expected := []message.Message{laoGreetMsg, laoCreateMSg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{laoCreateMSg}
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
	electiondID := "electionID"
	err = lite.StoreChannel(electiondID, "election", laoID)
	require.NoError(t, err)
	returnedKey, err = lite.GetLAOOrganizerPubKey(electiondID)
	require.NoError(t, err)
	require.True(t, organizerPubKey.Equal(returnedKey))
}

//======================================================================================================================
// LaoRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_GetRollCallState(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	lines := readJSONL(t, "rollCall_states.jsonl")
	states := []string{"create", "open", "close"}

	for i, line := range lines {
		msg := message.Message{
			Data:              base64.URLEncoding.EncodeToString(line),
			Sender:            "sender" + fmt.Sprint(i),
			Signature:         "sig" + fmt.Sprint(i),
			MessageID:         "ID" + fmt.Sprint(i),
			WitnessSignatures: []message.WitnessSignature{},
		}
		err = lite.StoreMessageAndData("channel1", msg)
		require.NoError(t, err)
		state, err := lite.GetRollCallState("channel1")
		require.NoError(t, err)
		require.Equal(t, states[i], state)
	}
}

func Test_SQLite_CheckPrevID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)
	prevIDs := []string{
		"fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U=",
		"krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk=",
	}
	states := []string{messagedata.RollCallActionCreate, messagedata.RollCallActionOpen}
	lines := readJSONL(t, "rollCall_states.jsonl")
	for i, line := range lines {
		msg := message.Message{
			Data:              base64.URLEncoding.EncodeToString(line),
			Sender:            "sender" + fmt.Sprint(i),
			Signature:         "sig" + fmt.Sprint(i),
			MessageID:         "ID" + fmt.Sprint(i),
			WitnessSignatures: []message.WitnessSignature{},
		}
		if i > 0 {
			ok, err := lite.CheckPrevID("channel1", prevIDs[i-1], states[i-1])
			require.NoError(t, err)
			require.True(t, ok)
		}
		err = lite.StoreMessageAndData("channel1", msg)
		require.NoError(t, err)
	}
}

func Test_SQLite_StoreChannelsAndMessage(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	channels := []string{"channel1", "channel2", "channel3"}
	attendees := []string{"attendee1", "attendee2", "attendee3"}
	laoID := "laoID"

	rollCallClose := messagedata.RollCallClose{
		Object:    messagedata.RollCallObject,
		Action:    messagedata.RollCallActionClose,
		UpdateID:  "updateID",
		Closes:    "closes",
		ClosedAt:  123456789,
		Attendees: attendees,
	}

	rollCallCloseBytes, err := json.Marshal(rollCallClose)
	require.NoError(t, err)

	rollCallCloseMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(rollCallCloseBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = lite.StoreChannelsAndMessage(channels, laoID, rollCallCloseMsg)
	require.NoError(t, err)

	expected := []message.Message{rollCallCloseMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	for _, channel := range channels {
		ok, err := lite.HasChannel(channel)
		require.NoError(t, err)
		require.True(t, ok)
	}
}

func Test_SQLite_StoreMessageWithElectionKey(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	laoID := "laoID"
	electionID := "electionID"
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	electionPubKey := point

	electionPubBuf, err := point.MarshalBinary()
	require.NoError(t, err)

	electionSetupBytes := readJSON(t, "election_setup.json")

	electionSetupMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(electionSetupBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

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

	err = lite.StoreMessageWithElectionKey(laoID, electionID, electionPubKey, secret, electionSetupMsg, electionKeyMsg)
	require.NoError(t, err)

	expected := []message.Message{electionSetupMsg}
	messages, err := lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{electionSetupMsg, electionKeyMsg}
	messages, err = lite.GetAllMessagesFromChannel(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	returnedSecretKey, err := lite.GetElectionSecretKey(electionID)
	require.NoError(t, err)
	require.True(t, secret.Equal(returnedSecretKey))
}

//======================================================================================================================
// ElectionRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_IsElectionStartedOrTerminated(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionID := "electionID"
	laoID := "laoID"
	ok, err := lite.IsElectionStartedOrEnded(electionID)
	require.NoError(t, err)
	require.False(t, ok)

	electionOpen := messagedata.ElectionOpen{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionOpen,
		Election: electionID,
		Lao:      laoID,
		OpenedAt: 123456789,
	}
	electionOpenBytes, err := json.Marshal(electionOpen)
	require.NoError(t, err)

	electionOpenMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(electionOpenBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

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

	electionClose := messagedata.ElectionEnd{
		Object:          messagedata.ElectionObject,
		Action:          messagedata.ElectionActionEnd,
		Election:        electionID,
		Lao:             laoID,
		CreatedAt:       123456789,
		RegisteredVotes: "votes",
	}
	electionCloseBytes, err := json.Marshal(electionClose)
	require.NoError(t, err)

	electionCloseMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(electionCloseBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

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

	electionID := "electionID"
	creationTime := int64(123456789)

	electionSetup := messagedata.ElectionSetup{
		Object:    "election",
		Action:    "setup",
		ID:        "electionSetupID",
		CreatedAt: creationTime,
		Version:   messagedata.OpenBallot,
	}

	electionSetupBytes, err := json.Marshal(electionSetup)
	require.NoError(t, err)

	electionSetupMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(electionSetupBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = lite.StoreMessageAndData(electionID, electionSetupMsg)
	require.NoError(t, err)

	returnedTime, err := lite.GetElectionCreationTime(electionID)
	require.NoError(t, err)
	require.Equal(t, creationTime, returnedTime)

	electionType, err := lite.GetElectionType(electionID)
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

	rollCallClose := messagedata.RollCallClose{
		Object:    messagedata.RollCallObject,
		Action:    messagedata.RollCallActionClose,
		UpdateID:  "updateID",
		Closes:    "closes",
		ClosedAt:  123456789,
		Attendees: attendees,
	}

	rollCallCloseBytes, err := json.Marshal(rollCallClose)
	require.NoError(t, err)

	rollCallCloseMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(rollCallCloseBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

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

	electionID := "electionID"
	laoID := "laoID"

	electionSetup := messagedata.ElectionSetup{
		Object:    "election",
		Action:    "setup",
		ID:        "electionSetupID",
		CreatedAt: 123456789,
		Version:   messagedata.OpenBallot,
		Questions: []messagedata.ElectionSetupQuestion{
			{
				ID:            "questionID1",
				Question:      "question1",
				VotingMethod:  "Plurality",
				BallotOptions: []string{"Option1", "Option2"},
			},
			{
				ID:            "questionID2",
				Question:      "question2",
				VotingMethod:  "Plurality",
				BallotOptions: []string{"Options1", "Options2", "Options3"},
			},
		},
	}

	expected, err := getQuestionsFromMessage(electionSetup)
	require.NoError(t, err)

	electionSetupBytes, err := json.Marshal(electionSetup)
	require.NoError(t, err)

	electionSetupMsg := newSQLiteMsg(electionSetupBytes, "sender", "messageID")

	err = lite.StoreMessageAndData(electionID, electionSetupMsg)
	require.NoError(t, err)

	questions, err := lite.GetElectionQuestions(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, questions)

	// Add votes to the election
	var castVotes []messagedata.VoteCastVote
	var votes []messagedata.Vote
	votes = append(votes, messagedata.Vote{ID: "voteID1", Question: "questionID1", Vote: "option1"})
	votes = append(votes, messagedata.Vote{ID: "voteID2", Question: "questionID2", Vote: "option1"})
	castVotes = append(castVotes, newVoteCastVote(laoID, electionID, 1, votes))

	votes = nil
	votes = append(votes, messagedata.Vote{ID: "voteID3", Question: "questionID1", Vote: "option1"})
	votes = append(votes, messagedata.Vote{ID: "voteID4", Question: "questionID2", Vote: "option3"})
	castVotes = append(castVotes, newVoteCastVote(laoID, electionID, 2, votes))

	votes = nil
	votes = append(votes, messagedata.Vote{ID: "voteID5", Question: "questionID1", Vote: "option2"})
	votes = append(votes, messagedata.Vote{ID: "voteID6", Question: "questionID2", Vote: "option2"})
	castVotes = append(castVotes, newVoteCastVote(laoID, electionID, 3, votes))

	for i, castVote := range castVotes {
		castVoteBytes, err := json.Marshal(castVote)
		require.NoError(t, err)
		err = lite.StoreMessageAndData(electionID, newSQLiteMsg(castVoteBytes, "sender", "messageID"+fmt.Sprint(i)))
		require.NoError(t, err)
	}

	votes = nil
	votes = append(votes, messagedata.Vote{ID: "voteID7", Question: "questionID1", Vote: "option1"})
	votes = append(votes, messagedata.Vote{ID: "voteID8", Question: "questionID2", Vote: "option3"})
	castVote := newVoteCastVote(laoID, electionID, 4, votes)
	castVoteBytes, err := json.Marshal(castVote)
	require.NoError(t, err)
	err = lite.StoreMessageAndData(electionID, newSQLiteMsg(castVoteBytes, "sender2", "messageID3"))
	require.NoError(t, err)

	question1 := expected["questionID1"]
	question1.ValidVotes = map[string]types.ValidVote{
		"sender":  {MsgID: "messageID2", ID: "voteID5", VoteTime: 3, Index: "option2"},
		"sender2": {MsgID: "messageID3", ID: "voteID7", VoteTime: 4, Index: "option1"},
	}

	question2 := expected["questionID2"]
	question2.ValidVotes = map[string]types.ValidVote{
		"sender":  {MsgID: "messageID2", ID: "voteID6", VoteTime: 3, Index: "option2"},
		"sender2": {MsgID: "messageID3", ID: "voteID8", VoteTime: 4, Index: "option3"},
	}

	expected["questionID1"] = question1
	expected["questionID2"] = question2

	questions, err = lite.GetElectionQuestionsWithValidVotes(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, questions)
}

func Test_SQLite_StoreMessageAndElectionResults(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	electionID := "electionID"
	laoID := "laoID"

	electionSetup := messagedata.ElectionSetup{
		Object:    "election",
		Action:    "setup",
		ID:        "electionSetupID",
		CreatedAt: 123456789,
		Version:   messagedata.OpenBallot,
		Questions: []messagedata.ElectionSetupQuestion{},
	}

	electionSetupBytes, err := json.Marshal(electionSetup)
	require.NoError(t, err)
	electionSetupMsg := newSQLiteMsg(electionSetupBytes, "sender", "messageID")

	electionKey := messagedata.ElectionKey{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionKey,
		Election: electionID,
		Key:      "key",
	}

	electionKeyBytes, err := json.Marshal(electionKey)
	require.NoError(t, err)
	electionKeyMsg := newSQLiteMsg(electionKeyBytes, "sender2", "messageID2")

	err = lite.StoreChannel(laoID, "election", "")
	require.NoError(t, err)

	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	err = lite.StoreMessageWithElectionKey(laoID, electionID, point, secret, electionSetupMsg, electionKeyMsg)
	require.NoError(t, err)

	expected := []message.Message{electionSetupMsg, electionKeyMsg}
	messages, err := lite.GetAllMessagesFromChannel(electionID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{electionSetupMsg}
	messages, err = lite.GetAllMessagesFromChannel(laoID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	returnedKey, err := lite.GetElectionSecretKey(electionID)
	require.NoError(t, err)
	require.True(t, secret.Equal(returnedKey))

}

//======================================================================================================================
// ChildRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_StoreChirpMessages(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	message1 := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("data1")),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message2 := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("data2")),
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

	chirpID := "chirpID"
	generalChirpID := "generalChirpID"

	err = lite.StoreChirpMessages(chirpID, generalChirpID, message1, message2)
	require.NoError(t, err)

	expected := []message.Message{message1}
	messages, err := lite.GetAllMessagesFromChannel(chirpID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)

	expected = []message.Message{message2}
	messages, err = lite.GetAllMessagesFromChannel(generalChirpID)
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

//======================================================================================================================
// ReactionRepository interface implementation tests
//======================================================================================================================

func Test_SQLite_IsAttendee(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	attendees := []string{"attendee1", "attendee2", "attendee3"}
	laoID := "laoID"

	rollCallClose := messagedata.RollCallClose{
		Object:    messagedata.RollCallObject,
		Action:    messagedata.RollCallActionClose,
		UpdateID:  "updateID",
		Closes:    "closes",
		ClosedAt:  123456789,
		Attendees: attendees,
	}

	rollCallCloseBytes, err := json.Marshal(rollCallClose)
	require.NoError(t, err)

	rollCallCloseMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(rollCallCloseBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

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

	var reactionAdd = messagedata.ReactionAdd{
		Object: messagedata.ReactionObject,
		Action: messagedata.ReactionActionAdd,
	}

	reactionAddBytes, err := json.Marshal(reactionAdd)
	require.NoError(t, err)

	reactionAddMsg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(reactionAddBytes),
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	sender, err := lite.GetReactionSender("ID1")
	require.NoError(t, err)
	require.Equal(t, "", sender)

	err = lite.StoreMessageAndData("channel1", reactionAddMsg)
	require.NoError(t, err)
	sender, err = lite.GetReactionSender("ID1")
	require.NoError(t, err)
	require.Equal(t, "sender1", sender)
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

func initMessages() []testMessage {
	message1 := message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message2 := message.Message{Data: "data2",
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message3 := message.Message{Data: "data3",
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

func readJSONL(t *testing.T, fileName string) [][]byte {
	file := filepath.Join("test_data/SQLite/", fileName)
	f, err := os.Open(file)
	require.NoError(t, err)
	defer f.Close()

	scanner := bufio.NewScanner(f)
	var lines [][]byte
	for scanner.Scan() {
		lines = append(lines, scanner.Bytes())

	}
	require.NoError(t, scanner.Err())
	return lines
}

func readJSON(t *testing.T, fileName string) []byte {
	file := filepath.Join("test_data/SQLite/", fileName)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)
	return buf
}

func newVoteCastVote(electionID, laoID string, createdAt int64, votes []messagedata.Vote) messagedata.VoteCastVote {
	return messagedata.VoteCastVote{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.VoteActionCastVote,
		Lao:       laoID,
		Election:  electionID,
		CreatedAt: createdAt,
		Votes:     votes,
	}
}

func newSQLiteMsg(data []byte, sender string, messageID string) message.Message {
	return message.Message{
		Data:              base64.URLEncoding.EncodeToString(data),
		Sender:            sender,
		Signature:         "signature",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}
}
