package handler

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/database"
	"testing"
	"time"
)

func Test_handleMessage(t *testing.T) {
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

	// Test 1: failed to handled message because message already exists

	msg := generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).Return(true, nil)

	args = append(args, input{
		name:     "Test 1",
		message:  msg,
		contains: "message " + msg.MessageID + " was already received",
	})

	// Test 2: failed to handled message because db is disconnected when querying if the message already exists

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())

	mockRepository.On("HasMessage", msg.MessageID).
		Return(false, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		message:  msg,
		contains: "DB is disconnected",
	})

	// Test 3: failed to handled message because the format of messageID

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	expectedMsgID := msg.MessageID
	msg.MessageID = base64.URLEncoding.EncodeToString([]byte("wrong messageID"))

	args = append(args, input{
		name:     "Test 3",
		message:  msg,
		contains: "messageID is wrong: expected " + expectedMsgID + " found " + msg.MessageID,
	})

	// Test 4: failed to handled message because wrong sender

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Sender = base64.URLEncoding.EncodeToString([]byte("wrong sender"))

	args = append(args, input{
		name:     "Test 4",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 5: failed to handled message because wrong data

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	args = append(args, input{
		name:     "Test 5",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 6: failed to handled message because wrong signature

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong signature"))

	args = append(args, input{
		name:     "Test 6",
		message:  msg,
		contains: "failed to verify signature",
	})

	// Test 7: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Signature = "wrong signature"

	args = append(args, input{
		name:     "Test 7",
		message:  msg,
		contains: "failed to decode signature",
	})

	// Test 8: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Sender = "wrong sender"

	args = append(args, input{
		name:     "Test 8",
		message:  msg,
		contains: "failed to decode public key",
	})

	// Test 9: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, keypair.Private, time.Now().Unix())
	msg.Data = "wrong data"

	args = append(args, input{
		name:     "Test 9",
		message:  msg,
		contains: "failed to decode data",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := HandleMessage(arg.channel, arg.message, false)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}

}
