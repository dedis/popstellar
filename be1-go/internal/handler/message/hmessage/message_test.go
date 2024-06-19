package hmessage

import (
	"encoding/base64"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"io"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/message/hmessage/mocks"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/test/generator"
	"testing"
	"time"
)

func Test_New(t *testing.T) {

}

func Test_handleChannel(t *testing.T) {
	db := mocks.NewRepository(t)
	channelHandler := mocks.NewChannelHandler(t)

	channelHandlers := make(ChannelHandlers)
	channelHandlers[channel.Root] = channelHandler

	msgHandler := New(db, channelHandlers, zerolog.New(io.Discard))

	_, publicBuf, private, _ := generator.GenerateKeyPair(t)
	sender := base64.URLEncoding.EncodeToString(publicBuf)

	type input struct {
		name        string
		channelPath string
		message     mmessage.Message
		isError     bool
		contains    string
	}

	args := make([]input, 0)

	// Test 1: failed to handled message because unknown channelPath type

	channelPath := "unknown"
	msg := generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return("", nil)

	args = append(args, input{
		name:        "Test 1",
		channelPath: channelPath,
		message:     msg,
		isError:     true,
		contains:    "unknown channelPath type for ",
	})

	// Test 2: failed to handled message because db is disconnected when querying the channelPath type

	channelPath = "disconnectedDB"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).
		Return("", xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:        "Test 2",
		channelPath: channelPath,
		message:     msg,
		isError:     true,
		contains:    "DB is disconnected",
	})

	// Test 3: failed to handled message because message already exists

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(true, nil)

	args = append(args, input{
		name:     "Test 3",
		message:  msg,
		isError:  true,
		contains: "was already received",
	})

	// Test 4: failed to handled message because db is disconnected when querying if the message already exists

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).
		Return(false, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 4",
		message:  msg,
		isError:  true,
		contains: "DB is disconnected",
	})

	// Test 5: failed to handled message because the format of messageID

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.MessageID = base64.URLEncoding.EncodeToString([]byte("wrong messageID"))

	args = append(args, input{
		name:     "Test 5",
		message:  msg,
		isError:  true,
		contains: "messageID is wrong",
	})

	// Test 6: failed to handled message because wrong sender

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Sender = base64.URLEncoding.EncodeToString([]byte("wrong sender"))

	args = append(args, input{
		name:     "Test 6",
		message:  msg,
		isError:  true,
		contains: "failed to verify signature",
	})

	// Test 7: failed to handled message because wrong data

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	args = append(args, input{
		name:     "Test 7",
		message:  msg,
		isError:  true,
		contains: "failed to verify signature",
	})

	// Test 8: failed to handled message because wrong signature

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Data = base64.URLEncoding.EncodeToString([]byte("wrong signature"))

	args = append(args, input{
		name:     "Test 8",
		message:  msg,
		isError:  true,
		contains: "failed to verify signature",
	})

	// Test 9: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Signature = "wrong signature"

	args = append(args, input{
		name:     "Test 9",
		message:  msg,
		isError:  true,
		contains: "failed to decode signature",
	})

	// Test 10: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Sender = "wrong sender"

	args = append(args, input{
		name:     "Test 10",
		message:  msg,
		isError:  true,
		contains: "failed to decode public key",
	})

	// Test 11: failed to handled message because wrong signature encoding

	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())
	msg.Data = "wrong data"

	args = append(args, input{
		name:     "Test 11",
		message:  msg,
		isError:  true,
		contains: "failed to decode data",
	})

	// Test 12: success to handled message for channel root

	channelPath = "/root"
	msg = generator.NewLaoCreateMsg(t, sender, "laoID", "laoName", time.Now().Unix(), sender, private)

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(channel.Root, nil).Once()
	channelHandler.On("Handle", channelPath, msg).Return(nil)

	args = append(args, input{
		name:        "Test 12",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err := msgHandler.Handle(arg.channelPath, arg.message, false)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}

		})
	}

}
