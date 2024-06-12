package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	mock2 "popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"strconv"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelLao(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	ownerPubBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(ownerPubBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	var args []input
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	laoID := base64.URLEncoding.EncodeToString([]byte("laoID"))
	err = subs.AddChannel(laoID)
	require.NoError(t, err)

	// Test 1:Success For LaoState message
	args = append(args, input{
		name:        "Test 1",
		msg:         newLaoStateMsg(t, ownerPubBuf64, laoID, mockRepository),
		channelPath: laoID,
		isError:     false,
		contains:    "",
	})

	creation := time.Now().Unix()
	start := creation + 2
	end := start + 1

	// Test 2: Error when RollCallCreate ID is not the expected hash
	args = append(args, input{
		name:        "Test 2",
		msg:         newRollCallCreateMsg(t, ownerPubBuf64, laoID, wrongLaoName, creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "roll call id is",
	})

	// Test 3: Error when RollCallCreate proposed start is before creation
	args = append(args, input{
		name:        "Test 3",
		msg:         newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, creation-1, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "roll call proposed start time should be greater than creation time",
	})

	// Test 4: Error when RollCallCreate proposed end is before proposed start
	args = append(args, input{
		name:        "Test 4",
		msg:         newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, start, start-1, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "roll call proposed end should be greater than proposed start",
	})

	// Test 5: Success for RollCallCreate message
	args = append(args, input{
		name:        "Test 5",
		msg:         newRollCallCreateMsg(t, ownerPubBuf64, laoID, goodLaoName, creation, start, end, false, mockRepository),
		channelPath: laoID,
		isError:     false,
		contains:    "",
	})

	opens := base64.URLEncoding.EncodeToString([]byte("opens"))
	wrongOpens := base64.URLEncoding.EncodeToString([]byte("wrongOpens"))

	// Test 6: Error when RollCallOpen ID is not the expected hash
	args = append(args, input{
		name:        "Test 6",
		msg:         newRollCallOpenMsg(t, ownerPubBuf64, laoID, wrongOpens, "", time.Now().Unix(), true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "roll call update id is",
	})

	// Test 7: Error when RollCallOpen opens is not the same as previous RollCallCreate
	args = append(args, input{
		name:        "Test 7",
		msg:         newRollCallOpenMsg(t, ownerPubBuf64, laoID, opens, wrongOpens, time.Now().Unix(), true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "previous id does not exist",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID2"))
	err = subs.AddChannel(laoID)
	require.NoError(t, err)

	// Test 8: Success for RollCallOpen message
	args = append(args, input{
		name:        "Test 8",
		msg:         newRollCallOpenMsg(t, ownerPubBuf64, laoID, opens, opens, time.Now().Unix(), false, mockRepository),
		channelPath: laoID,
		isError:     false,
		contains:    "",
	})

	closes := base64.URLEncoding.EncodeToString([]byte("closes"))
	wrongCloses := base64.URLEncoding.EncodeToString([]byte("wrongCloses"))

	// Test 9: Error when RollCallClose ID is not the expected hash
	args = append(args, input{
		name:        "Test 9",
		msg:         newRollCallCloseMsg(t, ownerPubBuf64, laoID, wrongCloses, "", time.Now().Unix(), true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "roll call update id is",
	})

	// Test 10: Error when RollCallClose closes is not the same as previous RollCallOpen
	args = append(args, input{
		name:        "Test 10",
		msg:         newRollCallCloseMsg(t, ownerPubBuf64, laoID, closes, wrongCloses, time.Now().Unix(), true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "previous id does not exist",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID3"))
	err = subs.AddChannel(laoID)
	require.NoError(t, err)

	// Test 11: Success for RollCallClose message
	args = append(args, input{
		name:        "Test 11",
		msg:         newRollCallCloseMsg(t, ownerPubBuf64, laoID, closes, closes, time.Now().Unix(), false, mockRepository),
		channelPath: laoID,
		isError:     false,
		contains:    "",
	})

	electionsName := "electionName"
	question := "question"
	wrongQuestion := "wrongQuestion"
	// Test 12: Error when sender is not the organizer of the lao for ElectionSetup
	args = append(args, input{
		name: "Test 12",
		msg: newElectionSetupMsg(t, ownerPublicKey, wrongSender, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "sender public key does not match organizer public key",
	})

	wrongLaoID := base64.URLEncoding.EncodeToString([]byte("wrongLaoID"))
	// Test 13: Error when ElectionSetup lao is not the same as the channelPath
	args = append(args, input{
		name: "Test 13",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, wrongLaoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "lao id is",
	})

	// Test 14: Error when ElectionSetup ID is not the expected hash
	args = append(args, input{
		name: "Test 14",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, "wrongName", question, messagedata.OpenBallot,
			creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "election id is",
	})

	// Test 15: Error when proposedStart is before createdAt
	args = append(args, input{
		name: "Test 15",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, creation-1, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "election start should be greater that creation time",
	})

	// Test 16: Error when proposedEnd is before proposedStart
	args = append(args, input{
		name: "Test 16",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, start-1, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "election end should be greater that start time",
	})

	// Test 17: Error when ElectionSetup question is empty
	args = append(args, input{
		name: "Test 17",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, "", messagedata.OpenBallot,
			creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "Question is empty",
	})

	//Test 18: Error when question hash is not the same as the expected hash
	args = append(args, input{
		name: "Test 18",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, wrongQuestion, messagedata.OpenBallot,
			creation, start, end, true, mockRepository),
		channelPath: laoID,
		isError:     true,
		contains:    "Question id is",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("laoID4"))
	err = subs.AddChannel(laoID)
	require.NoError(t, err)

	// Test 19: Success for ElectionSetup message
	args = append(args, input{
		name: "Test 19",
		msg: newElectionSetupMsg(t, ownerPublicKey, ownerPubBuf64, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, false, mockRepository),
		channelPath: laoID,
		isError:     false,
		contains:    "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err := handleChannelLao(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func newLaoStateMsg(t *testing.T, organizer, laoID string, mockRepository *mock2.Repository) message.Message {
	modificationID := base64.URLEncoding.EncodeToString([]byte("modificationID"))
	name := "laoName"
	creation := time.Now().Unix()
	lastModified := time.Now().Unix()

	msg := generator.NewLaoStateMsg(t, organizer, laoID, name, modificationID, creation, lastModified, nil)

	mockRepository.On("HasMessage", modificationID).
		Return(true, nil)
	mockRepository.On("GetLaoWitnesses", laoID).
		Return(map[string]struct{}{}, nil)
	mockRepository.On("StoreMessageAndData", laoID, msg).
		Return(nil)

	return msg
}

func newRollCallCreateMsg(t *testing.T, sender, laoID, laoName string, creation, start, end int64, isError bool,
	mockRepository *mock2.Repository) message.Message {

	createID := message.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		strconv.Itoa(int(creation)),
		goodLaoName,
	)

	msg := generator.NewRollCallCreateMsg(t, sender, laoName, createID, creation, start, end, nil)

	if !isError {
		mockRepository.On("StoreMessageAndData", laoID, msg).Return(nil)
	}

	return msg
}

func newRollCallOpenMsg(t *testing.T, sender, laoID, opens, prevID string, openedAt int64, isError bool,
	mockRepository *mock2.Repository) message.Message {

	openID := message.Hash(
		messagedata.RollCallFlag,
		strings.ReplaceAll(laoID, RootPrefix, ""),
		base64.URLEncoding.EncodeToString([]byte("opens")),
		strconv.Itoa(int(openedAt)),
	)

	msg := generator.NewRollCallOpenMsg(t, sender, openID, opens, openedAt, nil)

	if !isError {
		mockRepository.On("StoreMessageAndData", laoID, msg).Return(nil)
	}
	if prevID != "" {
		mockRepository.On("CheckPrevCreateOrCloseID", laoID, opens).Return(opens == prevID, nil)
	}

	return msg
}

func newRollCallCloseMsg(t *testing.T, sender, laoID, closes, prevID string, closedAt int64, isError bool,
	mockRepository *mock2.Repository) message.Message {

	closeID := message.Hash(
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
		mockRepository.On("StoreRollCallClose", channels, laoID, msg).Return(nil)
	}
	if prevID != "" {
		mockRepository.On("CheckPrevOpenOrReopenID", laoID, closes).Return(closes == prevID, nil)
	}

	return msg
}

func newElectionSetupMsg(t *testing.T, organizer kyber.Point, sender,
	setupLao, laoID, electionName, question, version string,
	createdAt, start, end int64,
	isError bool, mockRepository *mock2.Repository) message.Message {

	electionSetupID := message.Hash(
		messagedata.ElectionFlag,
		setupLao,
		strconv.Itoa(int(createdAt)),
		"electionName",
	)

	var questions []messagedata.ElectionSetupQuestion
	if question != "" {
		questionID := message.Hash("Question", electionSetupID, "question")
		questions = append(questions, messagedata.ElectionSetupQuestion{
			ID:            questionID,
			Question:      question,
			VotingMethod:  "Plurality",
			BallotOptions: []string{"Option1", "Option2"},
			WriteIn:       false,
		})
	} else {
		questionID := message.Hash("Question", electionSetupID, "")
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

	mockRepository.On("GetOrganizerPubKey", laoID).Return(organizer, nil)

	if !isError {
		mockRepository.On("StoreElection",
			laoID,
			laoID+"/"+electionSetupID,
			mock.AnythingOfType("*edwards25519.point"),
			mock.AnythingOfType("*edwards25519.scalar"),
			msg).Return(nil)
	}

	return msg
}