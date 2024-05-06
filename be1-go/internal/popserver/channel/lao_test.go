package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strconv"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelLao(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	ownerPubBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(ownerPubBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	err = config.SetConfig(t, ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")
	require.NoError(t, err)

	var args []input
	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	laoID := base64.URLEncoding.EncodeToString([]byte("laoID"))
	subs.AddChannel(laoID)

	// Test 1:Success For LaoState message
	args = append(args, input{
		name:     "Test 1",
		msg:      newLaoStateMsg(t, ownerPubBuf64, laoID, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	creation := time.Now().Unix()
	start := creation + 2
	end := start + 1

	// Test 2: Error when RollCallCreate ID is not the expected hash
	args = append(args, input{
		name:     "Test 2",
		msg:      newRollCallCreateMsg(t, ownerPubBuf64, laoID, wrongLaoName, creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call id is",
	})

	// Test 3: Error when RollCallCreate proposed start is before creation
	args = append(args, input{
		name:     "Test 3",
		msg:      newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, creation-1, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call proposed start time should be greater than creation time",
	})

	// Test 4: Error when RollCallCreate proposed end is before proposed start
	args = append(args, input{
		name:     "Test 4",
		msg:      newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, start, start-1, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call proposed end should be greater than proposed start",
	})

	// Test 5: Success for RollCallCreate message
	args = append(args, input{
		name:     "Test 5",
		msg:      newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, start, end, false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	opens := base64.URLEncoding.EncodeToString([]byte("opens"))
	wrongOpens := base64.URLEncoding.EncodeToString([]byte("wrongOpens"))

	// Test 6: Error when RollCallOpen ID is not the expected hash
	args = append(args, input{
		name:     "Test 6",
		msg:      newRollCallOpenMsg(t, ownerPubBuf64, laoID, wrongOpens, "", time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call update id is",
	})

	// Test 7: Error when RollCallOpen opens is not the same as previous RollCallCreate
	args = append(args, input{
		name:     "Test 7",
		msg:      newRollCallOpenMsg(t, ownerPubBuf64, laoID, opens, wrongOpens, time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "previous id does not exist",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID2"))
	subs.AddChannel(laoID)

	// Test 8: Success for RollCallOpen message
	args = append(args, input{
		name:     "Test 8",
		msg:      newRollCallOpenMsg(t, ownerPubBuf64, laoID, opens, opens, time.Now().Unix(), false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	closes := base64.URLEncoding.EncodeToString([]byte("closes"))
	wrongCloses := base64.URLEncoding.EncodeToString([]byte("wrongCloses"))

	// Test 9: Error when RollCallClose ID is not the expected hash
	args = append(args, input{
		name:     "Test 9",
		msg:      newRollCallCloseMsg(t, ownerPubBuf64, laoID, wrongCloses, "", time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call update id is",
	})

	// Test 10: Error when RollCallClose closes is not the same as previous RollCallOpen
	args = append(args, input{
		name:     "Test 10",
		msg:      newRollCallCloseMsg(t, ownerPubBuf64, laoID, closes, wrongCloses, time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "previous id does not exist",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID3"))
	subs.AddChannel(laoID)

	// Test 11: Success for RollCallClose message
	args = append(args, input{
		name:     "Test 11",
		msg:      newRollCallCloseMsg(t, ownerPubBuf64, laoID, closes, closes, time.Now().Unix(), false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	electionsName := "electionName"
	question := "question"
	wrongQuestion := "wrongQuestion"
	// Test 12: Error when sender is not the organizer of the lao for ElectionSetup
	args = append(args, input{
		name: "Test 12",
		msg: newElectionSetupMsg(t, ownerPublicKey, wrongSender, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "sender public key does not match organizer public key",
	})

	wrongLaoID := base64.URLEncoding.EncodeToString([]byte("wrongLaoID"))
	// Test 13: Error when ElectionSetup lao is not the same as the channel
	args = append(args, input{
		name: "Test 13",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, wrongLaoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "lao id is",
	})

	// Test 14: Error when ElectionSetup ID is not the expected hash
	args = append(args, input{
		name: "Test 14",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, "wrongName", question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election id is",
	})

	// Test 15: Error when proposedStart is before createdAt
	args = append(args, input{
		name: "Test 15",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, creation-1, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election start should be greater that creation time",
	})

	// Test 16: Error when proposedEnd is before proposedStart
	args = append(args, input{
		name: "Test 16",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, start-1, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election end should be greater that start time",
	})

	// Test 17: Error when ElectionSetup question is empty
	args = append(args, input{
		name: "Test 17",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, "", messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "Question is empty",
	})

	//Test 18: Error when question hash is not the same as the expected hash
	args = append(args, input{
		name: "Test 18",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, wrongQuestion, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "Question id is",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID4"))
	subs.AddChannel(laoID)

	// Test 19: Success for ElectionSetup message
	args = append(args, input{
		name: "Test 19",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelLao(arg.channel, arg.msg)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}

func newLaoStateMsg(t *testing.T, organizer, laoID string, mockRepo *database.MockRepository) message.Message {
	modificationID := base64.URLEncoding.EncodeToString([]byte("modificationID"))
	name := "laoName"
	creation := time.Now().Unix()
	lastModified := time.Now().Unix()

	msg := generator.NewLaoStateMsg(t, organizer, laoID, name, modificationID, creation, lastModified, nil)

	mockRepo.On("HasMessage", modificationID).
		Return(true, nil)
	mockRepo.On("GetLaoWitnesses", laoID).
		Return(map[string]struct{}{}, nil)
	mockRepo.On("StoreMessageAndData", laoID, msg).
		Return(nil)

	return msg
}

func newRollCallCreateMsg(t *testing.T, sender, laoID, laoName string, creation, start, end int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	createID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		strconv.Itoa(int(creation)),
		goodLaoName,
	)

	msg := generator.NewRollCallCreateMsg(t, sender, laoName, createID, creation, start, end, nil)

	if !isError {
		mockRepo.On("StoreMessageAndData", laoID, msg).Return(nil)
	}

	return msg
}

func newRollCallOpenMsg(t *testing.T, sender, laoID, opens, prevID string, openedAt int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	openID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		base64.URLEncoding.EncodeToString([]byte("opens")),
		strconv.Itoa(int(openedAt)),
	)

	msg := generator.NewRollCallOpenMsg(t, sender, openID, opens, openedAt, nil)

	if !isError {
		mockRepo.On("StoreMessageAndData", laoID, msg).Return(nil)
	}
	if prevID != "" {
		mockRepo.On("CheckPrevID", laoID, opens, messagedata.RollCallActionCreate).Return(opens == prevID, nil)
	}

	return msg
}

func newRollCallCloseMsg(t *testing.T, sender, laoID, closes, prevID string, closedAt int64, isError bool,
	mockRepo *database.MockRepository) message.Message {

	closeID := messagedata.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		base64.URLEncoding.EncodeToString([]byte("closes")),
		strconv.Itoa(int(closedAt)),
	)

	attendees := []string{base64.URLEncoding.EncodeToString([]byte("a")), base64.URLEncoding.EncodeToString([]byte("b"))}

	msg := generator.NewRollCallCloseMsg(t, sender, closeID, closes, closedAt, attendees, nil)

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

func newElectionSetupMsg(t *testing.T, organizer kyber.Point, sender,
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

	msg := generator.NewElectionSetupMsg(t, sender, electionSetupID, setupLao, electionName, version, createdAt, start,
		end, questions, nil)

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
