package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	database "popstellar/internal/popserver/database"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strconv"
	"strings"
	"testing"
	"time"
)

func NewLaoStateMsg(t *testing.T, organizer, laoID string, mockRepo *database.MockRepository) message.Message {
	modificationID := base64.URLEncoding.EncodeToString([]byte("modificationID"))
	laoState := messagedata.LaoState{
		Object:                 messagedata.LAOObject,
		Action:                 messagedata.LAOActionState,
		ID:                     laoID,
		Name:                   "laoName",
		Creation:               time.Now().Unix(),
		LastModified:           time.Now().Unix(),
		Organizer:              organizer,
		Witnesses:              []string{},
		ModificationID:         modificationID,
		ModificationSignatures: []messagedata.ModificationSignature{},
	}

	buf, err := json.Marshal(laoState)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	msg := newLaoMessage(t, buf64, organizer)

	mockRepo.On("HasMessage", modificationID).
		Return(true, nil)
	mockRepo.On("GetLaoWitnesses", laoID).
		Return(map[string]struct{}{}, nil)
	mockRepo.On("StoreMessage", laoID, msg).
		Return(nil)

	return msg
}

func NewRollCallCreateMsg(t *testing.T, sender, laoID, laoName string, creation, start, end int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	createID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		strconv.Itoa(int(creation)),
		GoodLaoName,
	)

	rollCallCreate := messagedata.RollCallCreate{
		Object:        messagedata.RollCallObject,
		Action:        messagedata.RollCallActionCreate,
		ID:            createID,
		Name:          laoName,
		Creation:      creation,
		ProposedStart: start,
		ProposedEnd:   end,
		Location:      "Location",
		Description:   "Description",
	}

	buf, err := json.Marshal(rollCallCreate)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	msg := newLaoMessage(t, buf64, sender)

	if !isError {
		mockRepo.On("StoreMessage", laoID, msg).Return(nil)
	}

	return msg
}

func NewRollCallOpenMsg(t *testing.T, sender, laoID, opens, prevID string, openedAt int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	openID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		base64.URLEncoding.EncodeToString([]byte("opens")),
		strconv.Itoa(int(openedAt)),
	)

	rollCallOpen := messagedata.RollCallOpen{
		Object:   messagedata.RollCallObject,
		Action:   messagedata.RollCallActionOpen,
		UpdateID: openID,
		Opens:    opens,
		OpenedAt: openedAt,
	}

	buf, err := json.Marshal(rollCallOpen)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	msg := newLaoMessage(t, buf64, sender)

	if !isError {
		mockRepo.On("StoreMessage", laoID, msg).Return(nil)
	}
	if prevID != "" {
		mockRepo.On("CheckPrevID", laoID, opens, messagedata.RollCallActionCreate).Return(opens == prevID, nil)
	}

	return msg
}

func NewRollCallCloseMsg(t *testing.T, sender, laoID, closes, prevID string, closedAt int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	closeID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		base64.URLEncoding.EncodeToString([]byte("closes")),
		strconv.Itoa(int(closedAt)),
	)

	attendees := []string{base64.URLEncoding.EncodeToString([]byte("a")), base64.URLEncoding.EncodeToString([]byte("b"))}
	rollCallClose := messagedata.RollCallClose{
		Object:    messagedata.RollCallObject,
		Action:    messagedata.RollCallActionClose,
		UpdateID:  closeID,
		Closes:    closes,
		ClosedAt:  closedAt,
		Attendees: attendees,
	}

	buf, err := json.Marshal(rollCallClose)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	msg := newLaoMessage(t, buf64, sender)

	if !isError {
		var channels []string
		for _, attendee := range attendees {
			channels = append(channels, laoID+Social+"/"+attendee)
		}
		mockRepo.On("StoreChannelsAndMessage", channels, laoID, msg).Return(nil)
	}
	if prevID != "" {
		mockRepo.On("CheckPrevID", laoID, closes, messagedata.RollCallActionOpen).Return(closes == prevID, nil)
	}

	return msg
}

func NewElectionSetupMsg(t *testing.T, organizer kyber.Point, sender,
	setupLao, laoID, electionName, question, version string,
	createdAt, start, end int64,
	isError bool, mockRepo *database.MockRepository) message.Message {

	electionSetupID := messagedata.Hash(
		messagedata.ElectionFlag,
		setupLao,
		strconv.Itoa(int(createdAt)),
		"electionName",
	)

	var questions []messagedata.ElectionSetupQuestion
	if question != "" {
		questionID := messagedata.Hash("Question", electionSetupID, "question")
		questions = append(questions, messagedata.ElectionSetupQuestion{
			ID:            questionID,
			Question:      question,
			VotingMethod:  "Plurality",
			BallotOptions: []string{"Option1", "Option2"},
			WriteIn:       false,
		})
	} else {
		questionID := messagedata.Hash("Question", electionSetupID, "")
		questions = append(questions, messagedata.ElectionSetupQuestion{
			ID:            questionID,
			Question:      "",
			VotingMethod:  "Plurality",
			BallotOptions: []string{"Option1", "Option2"},
			WriteIn:       false,
		})
	}

	electionSetup := messagedata.ElectionSetup{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionSetup,
		ID:        electionSetupID,
		Lao:       setupLao,
		Name:      electionName,
		Version:   version,
		CreatedAt: createdAt,
		StartTime: start,
		EndTime:   end,
		Questions: questions,
	}

	buf, err := json.Marshal(electionSetup)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	msg := newLaoMessage(t, buf64, sender)

	mockRepo.On("GetOrganizerPubKey", laoID).Return(organizer, nil)

	if !isError {
		mockRepo.On("StoreMessageWithElectionKey",
			laoID,
			laoID+"/"+electionSetupID,
			mock.AnythingOfType("*edwards25519.point"),
			mock.AnythingOfType("*edwards25519.scalar"),
			msg,
			mock.AnythingOfType("message.Message")).Return(nil)
	}

	return msg
}

func newLaoMessage(t *testing.T, data, sender string) message.Message {
	return message.Message{
		Data:              data,
		Sender:            sender,
		Signature:         "signature",
		MessageID:         "ID",
		WitnessSignatures: []message.WitnessSignature{},
	}
}
