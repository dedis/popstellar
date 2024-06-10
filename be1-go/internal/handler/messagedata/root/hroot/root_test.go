package hroot

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	messageHandler "popstellar/internal/handler/message/hmessage"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/messagedata/root/hroot/mocks"
	"popstellar/internal/state"
	generator2 "popstellar/internal/test/generator"
	"popstellar/internal/validation"
	"testing"
	"time"
)

const (
	ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	wrongSender   = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	goodLaoName   = "laoName"
	wrongLaoName  = "wrongLaoName"
)

func Test_handleChannelRoot(t *testing.T) {
	type input struct {
		name     string
		msg      mmessage.Message
		isError  bool
		contains string
	}

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	conf := state.CreateConfig(ownerPublicKey, serverPublicKey, serverSecretKey,
		"clientAddress", "serverAddress")
	db := mocks.NewRepository(t)

	subs := state.NewSubscribers()
	peers := state.NewPeers()

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	rootHandler := New(conf, db, subs, peers, schema)

	var args []input

	ownerPubBuf, err := ownerPublicKey.MarshalBinary()
	require.NoError(t, err)
	owner := base64.URLEncoding.EncodeToString(ownerPubBuf)

	// Test 1: error when different organizer and sender keys
	args = append(args, input{
		name:     "Test 1",
		msg:      newLaoCreateMsg(t, owner, wrongSender, goodLaoName, db, true),
		isError:  true,
		contains: "sender's public key does not match the organizer public key",
	})

	// Test 2: error when different sender and owner keys
	args = append(args, input{
		name:     "Test 2",
		msg:      newLaoCreateMsg(t, wrongSender, wrongSender, goodLaoName, db, true),
		isError:  true,
		contains: "sender's public key does not match the owner public key",
	})

	// Test 3: error when the lao name is not the same as the one used for the laoID
	args = append(args, input{
		name:     "Test 3",
		msg:      newLaoCreateMsg(t, owner, owner, wrongLaoName, db, true),
		isError:  true,
		contains: "invalid message field: lao id",
	})

	// Test 4: error when message data is not lao_create
	args = append(args, input{
		name:     "Test 4",
		msg:      generator2.NewNothingMsg(t, owner, nil),
		isError:  true,
		contains: "failed to validate schema",
	})

	// Test 5: success
	args = append(args, input{
		name:     "Test 5",
		msg:      newLaoCreateMsg(t, owner, owner, goodLaoName, db, false),
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err = rootHandler.Handle("", arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func newLaoCreateMsg(t *testing.T, organizer, sender, laoName string, mockRepository *mocks.Repository, isError bool) mmessage.Message {
	creation := time.Now().Unix()
	laoID := messagedata.Hash(
		organizer,
		fmt.Sprintf("%d", creation),
		goodLaoName,
	)

	msg := generator2.NewLaoCreateMsg(t, sender, laoID, laoName, creation, organizer, nil)

	mockRepository.On("HasChannel", RootPrefix+laoID).Return(false, nil)
	if !isError {
		laoPath := RootPrefix + laoID
		organizerBuf, err := base64.URLEncoding.DecodeString(organizer)
		require.NoError(t, err)
		channels := map[string]string{
			laoPath:                      messageHandler.LaoType,
			laoPath + Social + Chirps:    messageHandler.ChirpType,
			laoPath + Social + Reactions: messageHandler.ReactionType,
			laoPath + Consensus:          messageHandler.ConsensusType,
			laoPath + Coin:               messageHandler.CoinType,
			laoPath + Auth:               messageHandler.AuthType,
			laoPath + Federation:         messageHandler.FederationType,
		}
		mockRepository.On("StoreLaoWithLaoGreet",
			channels,
			laoPath,
			organizerBuf,
			msg, mock.AnythingOfType("mmessage.Message")).Return(nil)
	}
	return msg
}
