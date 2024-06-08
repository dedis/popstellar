package chirp

import (
	"encoding/base64"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/messagedata/root"
	"popstellar/internal/message/query/method/message"
	mock2 "popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/repository"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"strings"
	"testing"
	"time"
)

const ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="

func Test_handleChannelChirp(t *testing.T) {
	type input struct {
		name        string
		channelPath string
		msg         message.Message
		isError     bool
		contains    string
	}

	subs := state.NewSubscribers()

	db := mock2.NewRepository(t)

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	ownerPubBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(ownerPubBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	conf := state.CreateConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	chirp := New(conf, subs, db, schema)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	var args []input

	// Test 1: successfully add a chirp and notify it

	channelPath := "/root/lao1/social/" + sender

	args = append(args, input{
		name:        "Test 1",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, sender, time.Now().Unix(), db, false, subs),
		isError:     false,
		contains:    "",
	})

	// Test 2: failed to add chirp because not owner of the channelPath

	channelPath = "/root/lao2/social/" + sender

	args = append(args, input{
		name:        "Test 2",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, wrongSender, time.Now().Unix(), db, true, subs),
		isError:     true,
		contains:    "only the owner of the channelPath can post chirps",
	})

	// Test 3: failed to add chirp because negative timestamp

	channelPath = "/root/lao3/social/" + sender

	args = append(args, input{
		name:        "Test 3",
		channelPath: channelPath,
		msg:         newChirpAddMsg(t, channelPath, sender, -1, db, true, subs),
		isError:     true,
		contains:    "invalid message field",
	})

	// Test 4: successfully delete a chirp and notify it

	channelPath = "/root/lao4/social/" + sender

	args = append(args, input{
		name:        "Test 4",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, sender, chirpID, time.Now().Unix(), db, false, subs),
		isError:     false,
		contains:    "",
	})

	// Test 5: failed to delete chirp because not owner of the channelPath

	channelPath = "/root/lao5/social/" + sender

	args = append(args, input{
		name:        "Test 5",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, wrongSender, chirpID, time.Now().Unix(), db, true, subs),
		isError:     true,
		contains:    "only the owner of the channelPath can post chirps",
	})

	// Test 6: failed to delete chirp because negative timestamp

	channelPath = "/root/lao6/social/" + sender

	args = append(args, input{
		name:        "Test 6",
		channelPath: channelPath,
		msg:         newChirpDeleteMsg(t, channelPath, sender, chirpID, -1, db, true, subs),
		isError:     true,
		contains:    "invalid message field",
	})

	// Tests all cases

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err = chirp.Handle(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}

}

func newChirpAddMsg(t *testing.T, channelID string, sender string, timestamp int64,
	db *mock2.Repository, isError bool, subs repository.SubscriptionManager) message.Message {

	msg := generator.NewChirpAddMsg(t, sender, nil, timestamp)

	err := subs.AddChannel(channelID)
	require.NoError(t, err)

	if isError {
		return msg
	}

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, root.Social+"/"+msg.Sender)

	err = subs.AddChannel(chirpNotifyChannelID)
	require.NoError(t, err)

	db.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func newChirpDeleteMsg(t *testing.T, channelID string, sender string, chirpID string,
	timestamp int64, db *mock2.Repository, isError bool, subs repository.SubscriptionManager) message.Message {

	msg := generator.NewChirpDeleteMsg(t, sender, nil, chirpID, timestamp)

	err := subs.AddChannel(channelID)
	require.NoError(t, err)

	if isError {
		return msg
	}

	db.On("HasMessage", chirpID).Return(true, nil)

	chirpNotifyChannelID, _ := strings.CutSuffix(channelID, root.Social+"/"+msg.Sender)

	err = subs.AddChannel(chirpNotifyChannelID)
	require.NoError(t, err)

	db.On("StoreChirpMessages", channelID, chirpNotifyChannelID, mock.AnythingOfType("message.Message"),
		mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}
