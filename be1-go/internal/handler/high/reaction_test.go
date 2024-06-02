package high

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	state2 "popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelReaction(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state2.SetState(subs, peers, queries, hubParams)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	mockRepository := mocks.NewRepository(t)
	database.SetDatabase(mockRepository)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	//wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	invalidChirpID := "NotGooD"

	var args []input

	// Test 1: successfully add a reaction 👍

	laoID := "lao1"
	channelID := RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 1",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, time.Now().Unix(), mockRepository,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 2: successfully add a reaction 👎

	laoID = "lao2"
	channelID = RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 2",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👎", chirpID, time.Now().Unix(), mockRepository,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 3: successfully add a reaction ❤️

	laoID = "lao3"
	channelID = RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 3",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "❤️", chirpID, time.Now().Unix(), mockRepository,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 4: failed to add a reaction because wrong chirpID

	laoID = "lao4"
	channelID = RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 4",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", invalidChirpID, time.Now().Unix(), mockRepository,
			true, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 5: failed to add a reaction because negative timestamp

	laoID = "lao5"
	channelID = RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 5",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, -1, mockRepository,
			true, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 6: failed to add a reaction because didn't participate in roll-call

	laoID = "lao6"
	channelID = RootPrefix + laoID + Social + Reactions

	args = append(args, input{
		name:    "Test 6",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, time.Now().Unix(), mockRepository,
			false, true),
		isError:  true,
		contains: "user not inside roll-call",
	})

	// Test 7: successfully delete a reaction

	laoID = "lao7"
	channelID = RootPrefix + laoID + Social + Reactions
	reactionID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 7",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), mockRepository,
			false, false, false, false),
		isError:  false,
		contains: "",
	})

	// Test 8: failed to delete a reaction because negative timestamp

	laoID = "lao8"
	channelID = RootPrefix + laoID + Social + Reactions
	reactionID = "AAAAABu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 8",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, -1, mockRepository,
			true, false, false, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 9: failed to delete a reaction because reaction doesn't exist

	laoID = "lao9"
	channelID = RootPrefix + laoID + Social + Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 9",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), mockRepository,
			false, true, false, false),
		isError:  true,
		contains: "unknown reaction",
	})

	// Test 10: failed to delete a reaction because not owner

	laoID = "lao10"
	channelID = RootPrefix + laoID + Social + Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4KK="

	args = append(args, input{
		name:    "Test 10",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), mockRepository,
			false, false, true, false),
		isError:  true,
		contains: "only the owner of the reaction can delete it",
	})

	// Test 11: failed to delete a reaction because didn't participate in roll-call

	laoID = "lao11"
	channelID = RootPrefix + laoID + Social + Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dRYKK="

	args = append(args, input{
		name:    "Test 11",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), mockRepository,
			false, false, false, true),
		isError:  true,
		contains: "user not inside roll-call",
	})

	// Tests all cases

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelReaction(arg.channel, arg.msg)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}

}

func newReactionAddMsg(t *testing.T, channelID string, sender string, reactionCodePoint, chirpID string, timestamp int64,
	mockRepository *mocks.Repository, hasInvalidField, isNotAttendee bool) message.Message {

	msg := generator.NewReactionAddMsg(t, sender, nil, reactionCodePoint, chirpID, timestamp)

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	laoPath, _ := strings.CutSuffix(channelID, Social+Reactions)

	if !hasInvalidField && !isNotAttendee {
		mockRepository.On("IsAttendee", laoPath, sender).Return(true, nil)
		mockRepository.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if isNotAttendee {
		mockRepository.On("IsAttendee", laoPath, sender).Return(false, nil)
	}

	return msg
}

func newReactionDeleteMsg(t *testing.T, channelID string, sender string, reactionID string, timestamp int64,
	mockRepository *mocks.Repository, hasInvalidField, hasNotReaction, isNotOwner, isNotAttendee bool) message.Message {

	msg := generator.NewReactionDeleteMsg(t, sender, nil, reactionID, timestamp)

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	laoPath, _ := strings.CutSuffix(channelID, Social+Reactions)

	if !hasInvalidField && !hasNotReaction && !isNotOwner && !isNotAttendee {
		mockRepository.On("IsAttendee", laoPath, sender).Return(true, nil)

		mockRepository.On("GetReactionSender", reactionID).Return(sender, nil)

		mockRepository.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if hasNotReaction {
		mockRepository.On("IsAttendee", laoPath, sender).Return(true, nil)

		mockRepository.On("GetReactionSender", reactionID).Return("", nil)
	}

	if isNotOwner {
		mockRepository.On("IsAttendee", laoPath, sender).Return(true, nil)

		mockRepository.On("GetReactionSender", reactionID).Return("notSender", nil)
	}

	if isNotAttendee {
		mockRepository.On("IsAttendee", laoPath, sender).Return(false, nil)
	}

	return msg
}
