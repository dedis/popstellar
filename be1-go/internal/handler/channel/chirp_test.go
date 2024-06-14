package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method/message"
	mock2 "popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelChirp(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	var args []input

	// Test 1: successfully add a chirp and notify it

	channelPath := "/root/lao1/social/" + sender

	args = append(args, input{
		name:        "Test 1",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, sender, time.Now().Unix(), mockRepository, false),
		isError:     false,
		contains:    "",
	})

	// Test 2: failed to add chirp because not owner of the channelPath

	channelPath = "/root/lao2/social/" + sender

	args = append(args, input{
		name:        "Test 2",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, wrongSender, time.Now().Unix(), mockRepository, true),
		isError:     true,
		contains:    "only the owner of the channelPath can post chirps",
	})

	// Test 3: failed to add chirp because negative timestamp

	channelPath = "/root/lao3/social/" + sender

	args = append(args, input{
		name:        "Test 3",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, sender, -1, mockRepository, true),
		isError:     true,
		contains:    "invalid message field",
	})

	// Test 4: successfully delete a chirp and notify it

	channelPath = "/root/lao4/social/" + sender

	args = append(args, input{
		name:        "Test 4",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, sender, chirpID, time.Now().Unix(), mockRepository, false),
		isError:     false,
		contains:    "",
	})

	// Test 5: failed to delete chirp because not owner of the channelPath

	channelPath = "/root/lao5/social/" + sender

	args = append(args, input{
		name:        "Test 5",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, wrongSender, chirpID, time.Now().Unix(), mockRepository, true),
		isError:     true,
		contains:    "only the owner of the channelPath can post chirps",
	})

	// Test 6: failed to delete chirp because negative timestamp

	channelPath = "/root/lao6/social/" + sender

	args = append(args, input{
		name:        "Test 6",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, sender, chirpID, -1, mockRepository, true),
		isError:     true,
		contains:    "invalid message field",
	})

	// Tests all cases

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err = handleChannelChirp(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}

}

func newChirpAddMsg(t *testing.T, channelID string, sender string, timestamp int64,
	mockRepository *mock2.Repository, isError bool) message.Message {

	msg := generator.NewChirpAddMsg(t, sender, nil, timestamp)

	err := state.AddChannel(channelID)
	require.NoError(t, err)

	if isError {
		return msg
	}

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, Social+"/"+msg.Sender)

	err = state.AddChannel(chirpNotifyChannelID)
	require.NoError(t, err)

	mockRepository.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func newChirpDeleteMsg(t *testing.T, channelID string, sender string, chirpID string,
	timestamp int64, mockRepository *mock2.Repository, isError bool) message.Message {

	msg := generator.NewChirpDeleteMsg(t, sender, nil, chirpID, timestamp)

	err := state.AddChannel(channelID)
	require.NoError(t, err)

	if isError {
		return msg
	}

	mockRepository.On("HasMessage", chirpID).Return(true, nil)

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, Social+"/"+msg.Sender)

	err = state.AddChannel(chirpNotifyChannelID)
	require.NoError(t, err)

	mockRepository.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}