package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	database2 "popstellar/internal/popserver/database"
	"popstellar/message/query/method/message"
	"testing"
)

type input struct {
	name     string
	channel  string
	msg      message.Message
	isError  bool
	contains string
}

func Test_handleChannelRoot(t *testing.T) {
	var args []input
	mockRepo, err := database2.SetDatabase(t)
	require.NoError(t, err)

	ownerPubBuf, err := ownerPublicKey.MarshalBinary()
	require.NoError(t, err)
	owner := base64.URLEncoding.EncodeToString(ownerPubBuf)

	// Test 1: error when different organizer and sender keys
	args = append(args, input{
		name:     "Test 1",
		msg:      NewLaoCreateMsg(t, owner, WrongSender, GoodLaoName, mockRepo, true),
		isError:  true,
		contains: "sender's public key does not match the organizer public key",
	})

	// Test 2: error when different sender and owner keys
	args = append(args, input{
		name:     "Test 2",
		msg:      NewLaoCreateMsg(t, WrongSender, WrongSender, GoodLaoName, mockRepo, true),
		isError:  true,
		contains: "sender's public key does not match the owner public key",
	})

	// Test 3: error when the lao name is not the same as the one used for the laoID
	args = append(args, input{
		name:     "Test 3",
		msg:      NewLaoCreateMsg(t, owner, owner, WrongLaoName, mockRepo, true),
		isError:  true,
		contains: "failed to verify message data: invalid message field: lao id",
	})

	// Test 4: error when message data is not lao_create
	args = append(args, input{
		name:     "Test 4",
		msg:      NewNothingMsg(t, owner),
		isError:  true,
		contains: "failed to validate message against json schema",
	})

	// Test 5: success
	args = append(args, input{
		name:     "Test 5",
		msg:      NewLaoCreateMsg(t, owner, owner, GoodLaoName, mockRepo, false),
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelRoot(rootChannel, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}
