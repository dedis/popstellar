package handler

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/crypto"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	state "popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func Test_handleChannelElection(t *testing.T) {
	var args []input

	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)
	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	laoID := base64.URLEncoding.EncodeToString([]byte("laoID"))
	electionID := base64.URLEncoding.EncodeToString([]byte("electionID"))
	channelPath := "/root/" + laoID + "/" + electionID

	// Test 1 Error when ElectionOpen sender is not the same as the lao organizer
	args = append(args, input{
		name: "Test 1",
		msg: newElectionOpenMsg(t, ownerPublicKey, wrongSender, laoID, electionID, channelPath, "",
			-1, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	wrongChannelPath := "/root/" + base64.URLEncoding.EncodeToString([]byte("wrongLaoID")) + "/" + electionID

	// Test 2 Error when ElectionOpen lao id is not the same as the channel
	args = append(args, input{
		name: "Test 2",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "",
			-1, true, mockRepo),
		channel:  wrongChannelPath,
		isError:  true,
		contains: "lao id is not the same as the channel",
	})

	wrongChannelPath = "/root/" + laoID + "/" + base64.URLEncoding.EncodeToString([]byte("wrongElectionID"))

	// Test 3 Error when ElectionOpen election id is not the same as the channel
	args = append(args, input{
		name: "Test 3",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "",
			-1, true, mockRepo),
		channel:  wrongChannelPath,
		isError:  true,
		contains: "election id is not the same as the channel",
	})

	// Test 4 Error when Election is already started or ended
	args = append(args, input{
		name: "Test 4",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen,
			-1, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "election is already started or ended",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID2"))
	channelPath = "/root/" + laoID + "/" + electionID
	// Test 5 Error when ElectionOpen opened at before createdAt
	args = append(args, input{
		name: "Test 5",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionSetup,
			2, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "election open cannot have a creation time prior to election setup",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID3"))
	channelPath = "/root/" + laoID + "/" + electionID

	errAnswer := state.AddChannel(channelPath)
	require.Nil(t, errAnswer)

	// Test 6: Success when ElectionOpen is valid
	args = append(args, input{
		name: "Test 6",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionSetup,
			1, false, mockRepo),
		channel:  channelPath,
		isError:  false,
		contains: "",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("electionID4"))
	channelPath = "/root/" + laoID + "/" + electionID

	// Test 7 Error when ElectionEnd sender is not the same as the lao organizer
	args = append(args, input{
		name: "Test 7",
		msg: newElectionEndMsg(t, ownerPublicKey, wrongSender, laoID, electionID, channelPath, "", "",
			-1, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	wrongChannelPath = "/root/" + base64.URLEncoding.EncodeToString([]byte("wrongLaoID2")) + "/" + electionID

	// Test 8 Error when ElectionEnd lao id is not the same as the channel
	args = append(args, input{
		name: "Test 8",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, true, mockRepo),
		channel:  wrongChannelPath,
		isError:  true,
		contains: "lao id is not the same as the channel",
	})

	wrongChannelPath = "/root/" + laoID + "/" + base64.URLEncoding.EncodeToString([]byte("wrongElectionID2"))

	// Test 9 Error when ElectionEnd election id is not the same as the channel
	args = append(args, input{
		name: "Test 9",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, true, mockRepo),
		channel:  wrongChannelPath,
		isError:  true,
		contains: "election id is not the same as the channel",
	})

	// Test 10 Error when ElectionEnd is not started
	args = append(args, input{
		name: "Test 10",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionEnd, "",
			-1, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "election was not started",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID5"))
	channelPath = "/root/" + laoID + "/" + electionID

	// Test 11 Error when ElectionEnd creation time is before ElectionSetup creation time
	args = append(args, input{
		name: "Test 11",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, "",
			2, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "election end cannot have a creation time prior to election setup",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID6"))
	channelPath = "/root/" + laoID + "/" + electionID

	wrongVotes := messagedata.Hash("wrongVotes")

	// Test 12 Error when ElectionEnd is not the expected hash
	args = append(args, input{
		name: "Test 12",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, wrongVotes,
			1, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: fmt.Sprintf("registered votes is %s, should be sorted and equal to", wrongVotes),
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID7"))
	channelPath = "/root/" + laoID + "/" + electionID

	votes := messagedata.Hash("voteID1", "voteID2", "voteID3")

	errAnswer = state.AddChannel(channelPath)
	require.Nil(t, errAnswer)

	// Test 13: Success when ElectionEnd is valid
	args = append(args, input{
		name: "Test 13",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, votes,
			1, false, mockRepo),
		channel:  channelPath,
		isError:  false,
		contains: "",
	})

	//Test 14: Error when ElectionResult questions id are not base64 encoded
	result1 := []messagedata.ElectionResultQuestionResult{{BallotOption: messagedata.OpenBallot, Count: 1}}
	result2 := []messagedata.ElectionResultQuestionResult{{BallotOption: messagedata.OpenBallot, Count: 2}}
	questions := []messagedata.ElectionResultQuestion{{ID: "questionID1", Result: result1}, {ID: "questionID2", Result: result2}}

	args = append(args, input{
		name:     "Test 14",
		msg:      newElectionResultMsg(t, ownerPubBuf64, channelPath, questions, true, mockRepo),
		channel:  channelPath,
		isError:  true,
		contains: "failed to decode question id",
	})

	//Test 15: Success when ElectionResult is valid
	questions = []messagedata.ElectionResultQuestion{
		{ID: base64.URLEncoding.EncodeToString([]byte("questionID1")), Result: result1},
		{ID: base64.URLEncoding.EncodeToString([]byte("questionID2")), Result: result2},
	}
	args = append(args, input{
		name:     "Test 15",
		msg:      newElectionResultMsg(t, ownerPubBuf64, channelPath, questions, false, mockRepo),
		channel:  channelPath,
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelElection(arg.channel, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}

func newElectionOpenMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state string,
	createdAt int64, isError bool, mockRepo *database.MockRepository) message.Message {

	msg := generator.NewElectionOpenMsg(t, sender, laoID, electionID, 1, nil)

	mockRepo.On("GetLAOOrganizerPubKey", channelPath).Return(owner, nil)

	if createdAt >= 0 {
		mockRepo.On("GetElectionCreationTime", channelPath).Return(createdAt, nil)
	}

	if state != "" {
		mockRepo.On("IsElectionStartedOrEnded", channelPath).
			Return(state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil)
	}

	if !isError {
		mockRepo.On("StoreMessageAndData", channelPath, msg).Return(nil)
	}

	return msg
}

func newElectionEndMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state, votes string,
	createdAt int64, isError bool, mockRepo *database.MockRepository) message.Message {

	msg := generator.NewElectionCloseMsg(t, sender, laoID, electionID, votes, 1, nil)

	mockRepo.On("GetLAOOrganizerPubKey", channelPath).Return(owner, nil)

	if state != "" {
		mockRepo.On("IsElectionStarted", channelPath).
			Return(state == messagedata.ElectionActionOpen, nil)
	}

	if createdAt >= 0 {
		mockRepo.On("GetElectionCreationTime", channelPath).Return(createdAt, nil)
	}

	if votes != "" {
		questions := map[string]types.Question{
			"questionID1": {
				ID: []byte("questionID1"),
				ValidVotes: map[string]types.ValidVote{
					"voteID1": {
						ID: "voteID1",
					},
					"VoteID2": {
						ID: "voteID2",
					},
				},
			},
			"questionID2": {
				ID: []byte("questionID2"),
				ValidVotes: map[string]types.ValidVote{
					"voteID3": {
						ID: "voteID3",
					},
				},
			},
		}

		mockRepo.On("GetElectionQuestionsWithValidVotes", channelPath).Return(questions, nil)
	}

	if !isError {
		mockRepo.On("GetElectionType", channelPath).Return(messagedata.OpenBallot, nil)
		mockRepo.On("StoreMessageAndElectionResult", channelPath, msg, mock.AnythingOfType("message.Message")).
			Return(nil)
	}

	return msg
}

func newElectionResultMsg(t *testing.T, sender, channelPath string, questions []messagedata.ElectionResultQuestion,
	isError bool, mockRepo *database.MockRepository) message.Message {

	msg := generator.NewElectionResultMsg(t, sender, questions, nil)

	if !isError {
		mockRepo.On("StoreMessageAndData", channelPath, msg).Return(nil)
	}

	return msg
}
