package handler

import (
	"encoding/base64"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	state2 "popstellar/internal/singleton/state"
	"popstellar/internal/test/generatortest"
	types2 "popstellar/internal/types"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelChirp(t *testing.T) {
	subs := types2.NewSubscribers()
	queries := types2.NewQueries(&noLog)
	peers := types2.NewPeers()
	hubParams := types2.NewHubParams()

	state2.SetState(subs, peers, queries, hubParams)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	mockRepository := repository.NewMockRepository(t)
	database.SetDatabase(mockRepository)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	var args []input

	// Test 1: successfully add a chirp and notify it

	channelID := "/root/lao1/social/" + sender

	args = append(args, input{
		name:     "Test 1",
		channel:  channelID,
		msg:      newChirpAddMsg(t, channelID, sender, time.Now().Unix(), mockRepository, false),
		isError:  false,
		contains: "",
	})

	// Test 2: failed to add chirp because not owner of the channel

	channelID = "/root/lao2/social/" + sender

	args = append(args, input{
		name:     "Test 2",
		channel:  channelID,
		msg:      newChirpAddMsg(t, channelID, wrongSender, time.Now().Unix(), mockRepository, true),
		isError:  true,
		contains: "only the owner of the channel can post chirps",
	})

	// Test 3: failed to add chirp because negative timestamp

	channelID = "/root/lao3/social/" + sender

	args = append(args, input{
		name:     "Test 3",
		channel:  channelID,
		msg:      newChirpAddMsg(t, channelID, sender, -1, mockRepository, true),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 4: successfully delete a chirp and notify it

	channelID = "/root/lao4/social/" + sender

	args = append(args, input{
		name:     "Test 4",
		channel:  channelID,
		msg:      newChirpDeleteMsg(t, channelID, sender, chirpID, time.Now().Unix(), mockRepository, false),
		isError:  false,
		contains: "",
	})

	// Test 5: failed to delete chirp because not owner of the channel

	channelID = "/root/lao5/social/" + sender

	args = append(args, input{
		name:     "Test 5",
		channel:  channelID,
		msg:      newChirpDeleteMsg(t, channelID, wrongSender, chirpID, time.Now().Unix(), mockRepository, true),
		isError:  true,
		contains: "only the owner of the channel can post chirps",
	})

	// Test 6: failed to delete chirp because negative timestamp

	channelID = "/root/lao6/social/" + sender

	args = append(args, input{
		name:     "Test 6",
		channel:  channelID,
		msg:      newChirpDeleteMsg(t, channelID, sender, chirpID, -1, mockRepository, true),
		isError:  true,
		contains: "invalid message field",
	})

	// Tests all cases

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelChirp(arg.channel, arg.msg)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}

}

func newChirpAddMsg(t *testing.T, channelID string, sender string, timestamp int64,
	mockRepository *repository.MockRepository, isError bool) message.Message {

	msg := generatortest.NewChirpAddMsg(t, sender, nil, timestamp)

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	if isError {
		return msg
	}

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, Social+"/"+msg.Sender)

	errAnswer = state2.AddChannel(chirpNotifyChannelID)
	require.Nil(t, errAnswer)

	mockRepository.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func newChirpDeleteMsg(t *testing.T, channelID string, sender string, chirpID string,
	timestamp int64, mockRepository *repository.MockRepository, isError bool) message.Message {

	msg := generatortest.NewChirpDeleteMsg(t, sender, nil, chirpID, timestamp)

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	if isError {
		return msg
	}

	mockRepository.On("HasMessage", chirpID).Return(true, nil)

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, Social+"/"+msg.Sender)

	errAnswer = state2.AddChannel(chirpNotifyChannelID)
	require.Nil(t, errAnswer)

	mockRepository.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}
