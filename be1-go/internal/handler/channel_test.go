package handler

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/database"
	"testing"
	"time"
)

// the public key used in every lao_create json files in the test_data/root folder
const ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="

func Test_handleChannel(t *testing.T) {
	mockRepository := mocks.NewRepository(t)
	database.SetDatabase(mockRepository)

	keypair := GenerateKeyPair(t)
	sender := base64.URLEncoding.EncodeToString(keypair.PublicBuf)

	type input struct {
		name     string
		channel  string
		message  message.Message
		contains string
	}

	args := make([]input, 0)

	// Test 1: failed to handled message because unknown channel type

	channel := "unknown"
	msg := generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", channel).Return("", nil)

	args = append(args, input{
		name:     "Test 1",
		channel:  channel,
		message:  msg,
		contains: "unknown channel type for " + channel,
	})

	// Test 2: failed to handled message because db is disconnected when querying the channel type

	channel = "disconnectedDB"
	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", channel).
		Return("", xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		channel:  channel,
		message:  msg,
		contains: "DB is disconnected",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := HandleMessage(arg.channel, arg.message, false)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}

}

type Keypair struct {
	Public     kyber.Point
	PublicBuf  []byte
	Private    kyber.Scalar
	PrivateBuf []byte
}

func GenerateKeyPair(t *testing.T) Keypair {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	publicBuf, err := point.MarshalBinary()
	require.NoError(t, err)
	privateBuf, err := secret.MarshalBinary()
	require.NoError(t, err)

	return Keypair{point, publicBuf, secret, privateBuf}
}
