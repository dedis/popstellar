package channel

import (
	"encoding/base64"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"io"
	"os"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/utils"
	"popstellar/message/query/method/message"
	"popstellar/validation"
	"testing"
	"time"
)

// the public key used in every lao_create json files in the test_data/root folder
const ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="

var noLog = zerolog.New(io.Discard)

func TestMain(m *testing.M) {
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	utils.InitUtils(&noLog, schemaValidator)

	exitVal := m.Run()

	os.Exit(exitVal)
}

func Test_handleChannel(t *testing.T) {
	mockRepository, err := database.SetDatabase(t)
	require.NoError(t, err)

	keypair := popserver.GenerateKeyPair(t)
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

	// Test 3: failed to handled message because message already exists

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).Return(true, nil)

	args = append(args, input{
		name:     "Test 3",
		message:  msg,
		contains: "message " + msg.MessageID + " was already received",
	})

	// Test 4: failed to handled message because db is disconnected when querying if the message already exists

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).
		Return(false, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 4",
		message:  msg,
		contains: "DB is disconnected",
	})

	// Test 5: failed to handled message because the format of messageID

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	expectedMsgID := msg.MessageID
	msg.MessageID = base64.URLEncoding.EncodeToString([]byte("wrong messageID"))

	args = append(args, input{
		name:     "Test 5",
		message:  msg,
		contains: "messageID is wrong: expected " + expectedMsgID + " found " + msg.MessageID,
	})

	// Test 6: failed to handled message because wrong sender

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Sender = base64.URLEncoding.EncodeToString([]byte("wrong sender"))

	args = append(args, input{
		name:     "Test 6",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 7: failed to handled message because wrong data

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	args = append(args, input{
		name:     "Test 7",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 8: failed to handled message because wrong signature

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong signature"))

	args = append(args, input{
		name:     "Test 8",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 9: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Signature = "wrong signature"

	args = append(args, input{
		name:     "Test 9",
		message:  msg,
		contains: "failed to decode signature",
	})

	// Test 10: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Sender = "wrong sender"

	args = append(args, input{
		name:     "Test 10",
		message:  msg,
		contains: "failed to decode public key",
	})

	// Test 11: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = "wrong data"

	args = append(args, input{
		name:     "Test 11",
		message:  msg,
		contains: "failed to decode data",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := HandleChannel(arg.channel, arg.message)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}

}
