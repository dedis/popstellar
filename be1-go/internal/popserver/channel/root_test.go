package channel

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

const (
	// wrongSender A public key different from the owner public key
	wrongSender  = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	goodLaoName  = "laoName"
	wrongLaoName = "wrongLaoName"
)

type input struct {
	name     string
	channel  string
	msg      message.Message
	isError  bool
	contains string
}

func Test_handleChannelRoot(t *testing.T) {
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

	var args []input
	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	ownerPubBuf, err := ownerPublicKey.MarshalBinary()
	require.NoError(t, err)
	owner := base64.URLEncoding.EncodeToString(ownerPubBuf)

	// Test 1: error when different organizer and sender keys
	args = append(args, input{
		name:     "Test 1",
		msg:      newLaoCreateMsg(t, owner, wrongSender, goodLaoName, mockRepo, true),
		isError:  true,
		contains: "sender's public key does not match the organizer public key",
	})

	// Test 2: error when different sender and owner keys
	args = append(args, input{
		name:     "Test 2",
		msg:      newLaoCreateMsg(t, wrongSender, wrongSender, goodLaoName, mockRepo, true),
		isError:  true,
		contains: "sender's public key does not match the owner public key",
	})

	// Test 3: error when the lao name is not the same as the one used for the laoID
	args = append(args, input{
		name:     "Test 3",
		msg:      newLaoCreateMsg(t, owner, owner, wrongLaoName, mockRepo, true),
		isError:  true,
		contains: "failed to verify message data: invalid message field: lao id",
	})

	// Test 4: error when message data is not lao_create
	args = append(args, input{
		name:     "Test 4",
		msg:      generator.NewNothingMsg(t, owner, nil),
		isError:  true,
		contains: "failed to validate message against json schema",
	})

	// Test 5: success
	args = append(args, input{
		name:     "Test 5",
		msg:      newLaoCreateMsg(t, owner, owner, goodLaoName, mockRepo, false),
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelRoot(rootChannel, arg.msg)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}

func newLaoCreateMsg(t *testing.T, organizer, sender, laoName string, mockRepo *database.MockRepository, isError bool) message.Message {
	creation := time.Now().Unix()
	laoID := messagedata.Hash(
		organizer,
		fmt.Sprintf("%d", creation),
		goodLaoName,
	)

	msg := generator.NewLaoCreateMsg(t, sender, laoID, laoName, creation, organizer, nil)

	mockRepo.On("HasChannel", RootPrefix+laoID).Return(false, nil)
	if !isError {
		laoPath := RootPrefix + laoID
		organizerBuf, err := base64.URLEncoding.DecodeString(organizer)
		require.NoError(t, err)
		channels := map[string]string{
			laoPath + Social + Chirps:    ChannelChirp,
			laoPath + Social + Reactions: ChannelReaction,
			laoPath + Consensus:          ChannelConsensus,
			laoPath + Coin:               ChannelCoin,
			laoPath + Auth:               ChannelAuth,
		}
		mockRepo.On("StoreChannelsAndMessageWithLaoGreet",
			channels,
			laoPath,
			organizerBuf,
			msg, mock.AnythingOfType("message.Message")).Return(nil)
	}
	return msg
}
