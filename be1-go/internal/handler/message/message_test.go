package message

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"testing"
	"time"
)

// nullChannelSubHandler is a struct that implements the DataHandler interface with no-op methods
type nullChannelSubHandler struct{}

// handle method for nullChannelSubHandler that always returns nil
func (n *nullChannelSubHandler) Handle(_ string, _ message.Message) error {
	return nil
}

func Test_handleChannel(t *testing.T) {
	db := mock.NewRepository(t)

	subHandlers := DataHandlers{
		Root:       &nullChannelSubHandler{},
		Lao:        &nullChannelSubHandler{},
		Election:   &nullChannelSubHandler{},
		Chirp:      &nullChannelSubHandler{},
		Reaction:   &nullChannelSubHandler{},
		Coin:       &nullChannelSubHandler{},
		Federation: &nullChannelSubHandler{},
	}

	channel := New(db, subHandlers)

	_, publicBuf, private, _ := generator.GenerateKeyPair(t)
	sender := base64.URLEncoding.EncodeToString(publicBuf)

	type input struct {
		name        string
		channelPath string
		message     message.Message
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

	channelPath = "rootMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(RootType, nil)

	args = append(args, input{
		name:        "Test 12",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 13: success to handled message for channel lao

	channelPath = "laoMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(LaoType, nil)

	args = append(args, input{
		name:        "Test 13",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 14: success to handled message for channel election

	channelPath = "electionMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(LaoType, nil)

	args = append(args, input{
		name:        "Test 14",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 15: success to handled message for channel chirp

	channelPath = "chirpMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(ChirpType, nil)

	args = append(args, input{
		name:        "Test 15",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 16: success to handled message for channel reaction

	channelPath = "reaction"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(ReactionType, nil)

	args = append(args, input{
		name:        "Test 16",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 17: success to handled message for channel coin

	channelPath = "coinMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(CoinType, nil)

	args = append(args, input{
		name:        "Test 17",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	// Test 18: success to handled message for channel coin

	channelPath = "coinMsg"
	msg = generator.NewChirpAddMsg(t, sender, private, time.Now().Unix())

	db.On("HasMessage", msg.MessageID).Return(false, nil)
	db.On("GetChannelType", channelPath).Return(FederationType, nil)

	args = append(args, input{
		name:        "Test 18",
		channelPath: channelPath,
		message:     msg,
		isError:     false,
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err := channel.Handle(arg.channelPath, arg.message, false)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}

		})
	}

}
