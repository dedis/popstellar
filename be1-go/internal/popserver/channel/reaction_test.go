package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleChannelReaction(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	err = config.SetConfig(t, ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")
	require.NoError(t, err)

	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	//wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	invalidChirpID := "NotGooD"

	var args []input

	// Test 1: successfully add a reaction 👍

	laoID := "lao1"
	channelID := "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 1",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "👍", chirpID, time.Now().Unix(), mockRepo,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 2: successfully add a reaction 👎

	laoID = "lao2"
	channelID = "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 2",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "👎", chirpID, time.Now().Unix(), mockRepo,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 3: successfully add a reaction ❤️

	laoID = "lao3"
	channelID = "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 3",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "❤️", chirpID, time.Now().Unix(), mockRepo,
			false, false),
		isError:  false,
		contains: "",
	})

	// Test 4: failed to add a reaction because wrong chirpID

	laoID = "lao4"
	channelID = "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 4",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "👍", invalidChirpID, time.Now().Unix(), mockRepo,
			true, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 5: failed to add a reaction because negative timestamp

	laoID = "lao5"
	channelID = "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 5",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "👍", chirpID, -1, mockRepo,
			true, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 6: failed to add a reaction because didn't participate in roll-call

	laoID = "lao6"
	channelID = "/root/" + laoID + "/social/" + sender

	args = append(args, input{
		name:    "Test 6",
		channel: channelID,
		msg: newReactionAddMsg(t, channelID, laoID, sender, "👍", chirpID, time.Now().Unix(), mockRepo,
			false, true),
		isError:  true,
		contains: "user not inside roll-call",
	})

	// Test 7: successfully delete a reaction

	laoID = "lao7"
	channelID = "/root/" + laoID + "/social/" + sender
	reactionID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 7",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, laoID, sender, reactionID, time.Now().Unix(), mockRepo,
			false, false, false, false),
		isError:  false,
		contains: "",
	})

	// Test 8: failed to delete a reaction because negative timestamp

	laoID = "lao8"
	channelID = "/root/" + laoID + "/social/" + sender
	reactionID = "AAAAABu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 8",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, laoID, sender, reactionID, -1, mockRepo,
			true, false, false, false),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 9: failed to delete a reaction because reaction doesn't exist

	laoID = "lao9"
	channelID = "/root/" + laoID + "/social/" + sender
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:    "Test 9",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, laoID, sender, reactionID, time.Now().Unix(), mockRepo,
			false, true, false, false),
		isError:  true,
		contains: "unknown reaction",
	})

	// Test 10: failed to delete a reaction because not owner

	laoID = "lao10"
	channelID = "/root/" + laoID + "/social/" + sender
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4KK="

	args = append(args, input{
		name:    "Test 10",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, laoID, sender, reactionID, time.Now().Unix(), mockRepo,
			false, false, true, false),
		isError:  true,
		contains: "only the owner of the reaction can delete it",
	})

	// Test 11: failed to delete a reaction because didn't participate in roll-call

	laoID = "lao11"
	channelID = "/root/" + laoID + "/social/" + sender
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dRYKK="

	args = append(args, input{
		name:    "Test 11",
		channel: channelID,
		msg: newReactionDeleteMsg(t, channelID, laoID, sender, reactionID, time.Now().Unix(), mockRepo,
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

func newReactionAddMsg(t *testing.T, channelID, laoID string, sender string, reactionCodePoint, chirpID string, timestamp int64,
	mockRepo *database.MockRepository, hasInvalidField, isNotAttendee bool) message.Message {

	msg := generator.NewReactionAddMsg(t, sender, nil, reactionCodePoint, chirpID, timestamp)

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	if !hasInvalidField && !isNotAttendee {
		mockRepo.On("IsAttendee", laoID, sender).Return(true, nil)
		mockRepo.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if isNotAttendee {
		mockRepo.On("IsAttendee", laoID, sender).Return(false, nil)
	}

	return msg
}

func newReactionDeleteMsg(t *testing.T, channelID, laoID string, sender string, reactionID string, timestamp int64,
	mockRepo *database.MockRepository, hasInvalidField, hasNotReaction, isNotOwner, isNotAttendee bool) message.Message {

	msg := generator.NewReactionDeleteMsg(t, sender, nil, reactionID, timestamp)

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	if !hasInvalidField && !hasNotReaction && !isNotOwner && !isNotAttendee {
		mockRepo.On("IsAttendee", laoID, sender).Return(true, nil)

		mockRepo.On("GetReactionSender", reactionID).Return(sender, nil)

		mockRepo.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if hasNotReaction {
		mockRepo.On("IsAttendee", laoID, sender).Return(true, nil)

		mockRepo.On("GetReactionSender", reactionID).Return("", nil)
	}

	if isNotOwner {
		mockRepo.On("IsAttendee", laoID, sender).Return(true, nil)

		mockRepo.On("GetReactionSender", reactionID).Return("notSender", nil)
	}

	if isNotAttendee {
		mockRepo.On("IsAttendee", laoID, sender).Return(false, nil)
	}

	return msg
}
