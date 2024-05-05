package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func newElectionOpenMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state string,
	createdAt int64, isError bool, mockRepo *database.MockRepository) message.Message {
	electionOpen := messagedata.ElectionOpen{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionOpen,
		Lao:      laoID,
		Election: electionID,
		OpenedAt: 1,
	}

	buf, err := json.Marshal(electionOpen)
	require.NoError(t, err)

	mockRepo.On("GetLAOOrganizerPubKey", channelPath).Return(owner, nil)

	if createdAt >= 0 {
		mockRepo.On("GetElectionCreationTime", channelPath).Return(createdAt, nil)
	}

	if state != "" {
		mockRepo.On("IsElectionStartedOrEnded", channelPath).
			Return(state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil)
	}

	msg := newElectionMessage(t, buf, sender)

	if !isError {
		mockRepo.On("StoreMessage", channelPath, msg).Return(nil)
	}

	return msg
}

func newElectionEndMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state, votes string,
	createdAt int64, isError bool, mockRepo *database.MockRepository) message.Message {

	electionEnd := messagedata.ElectionEnd{
		Object:          messagedata.ElectionObject,
		Action:          messagedata.ElectionActionEnd,
		Lao:             laoID,
		Election:        electionID,
		CreatedAt:       1,
		RegisteredVotes: votes,
	}

	buf, err := json.Marshal(electionEnd)
	require.NoError(t, err)

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

	msg := newElectionMessage(t, buf, sender)

	if !isError {
		mockRepo.On("GetElectionType", channelPath).Return(messagedata.OpenBallot, nil)
		mockRepo.On("StoreMessageAndElectionResult", channelPath, msg, mock.AnythingOfType("message.Message")).
			Return(nil)
	}

	return msg
}

func newElectionResultMsg(t *testing.T, sender, channelPath string, questions []messagedata.ElectionResultQuestion,
	isError bool, mockRepo *database.MockRepository) message.Message {

	electionResult := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: questions,
	}

	buf, err := json.Marshal(electionResult)
	require.NoError(t, err)

	msg := newElectionMessage(t, buf, sender)

	if !isError {
		mockRepo.On("StoreMessage", channelPath, msg).Return(nil)
	}

	return msg
}

func newElectionMessage(t *testing.T, data []byte, sender string) message.Message {
	return message.Message{
		Data:              base64.URLEncoding.EncodeToString(data),
		Sender:            sender,
		Signature:         "signature",
		MessageID:         "ID",
		WitnessSignatures: []message.WitnessSignature{},
	}
}
