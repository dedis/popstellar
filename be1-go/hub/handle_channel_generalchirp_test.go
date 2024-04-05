package hub

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
)

const messageDataPath string = "../validation/protocol/examples/messageData/"

type inputTestHandleChannelGeneralChirp struct {
	name      string
	params    handlerParameters
	channelID string
	message   message.Message
	hasError  bool
	sockets   []*fakeSocket
}

func Test_handleChannelGeneralChirp(t *testing.T) {
	inputs := make([]inputTestHandleChannelGeneralChirp, 0)

	inputs = append(inputs, newSuccessTestHandleChannelGeneralChirp(t,
		"chirp_notify_add/chirp_notify_add.json",
		"send chirp notify add"))

	inputs = append(inputs, newFailTestHandleChannelGeneralChirp(t,
		"chirp_notify_add/wrong_chirp_notify_add_negative_time.json",
		"send chirp notify add negative time"))

	inputs = append(inputs, newFailTestHandleChannelGeneralChirp(t,
		"chirp_notify_add/wrong_chirp_notify_add_not_base_64_chirp_id.json",
		"send chirp notify add wrong chirp id"))

	inputs = append(inputs, newSuccessTestHandleChannelGeneralChirp(t,
		"chirp_notify_delete/chirp_notify_delete.json",
		"send chirp notify delete"))

	inputs = append(inputs, newFailTestHandleChannelGeneralChirp(t,
		"chirp_notify_delete/wrong_chirp_notify_delete_negative_time.json",
		"send chirp notify delete negative time"))

	inputs = append(inputs, newFailTestHandleChannelGeneralChirp(t,
		"chirp_notify_delete/wrong_chirp_notify_delete_not_base_64_chirp_id.json",
		"send chirp notify delete wrong chirp id"))

	inputInvalidSender := newFailTestHandleChannelGeneralChirp(t,
		"chirp_notify_add/chirp_notify_add.json",
		"send chirp notify add not sent by server")
	inputInvalidSender.message.Sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDA="
	inputs = append(inputs, inputInvalidSender)

	// Tests all cases

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleChannelGeneralChirp(i.params, i.channelID, i.message)
			if i.hasError {
				require.NotNil(t, errAnswer)
			} else {
				require.Nil(t, errAnswer)

				for _, s := range i.sockets {
					require.NotNil(t, s.msg)

					var msg method.Broadcast
					err := json.Unmarshal(s.msg, &msg)
					require.NoError(t, err)

					require.Equal(t, i.message, msg.Params.Message)
				}
			}
		})
	}

}

func newSuccessTestHandleChannelGeneralChirp(t *testing.T, filename string, name string) inputTestHandleChannelGeneralChirp {
	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/social/chirps"

	file := filepath.Join(messageDataPath, filename)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	mockRepo := NewMockRepository(t)
	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	require.NoError(t, err)
	mockRepo.On("GetServerPubKey").Return(senderBuf, nil)
	mockRepo.On("StoreMessage", channelID, m).Return(nil)

	sockets := []*fakeSocket{
		{id: "0"},
		{id: "1"},
		{id: "2"},
		{id: "3"},
	}

	params := newHandlerParametersWithFakeSocket(mockRepo, sockets[0])
	params.subs.addChannel(channelID)

	for _, s := range sockets {
		err := params.subs.subscribe(channelID, s)
		require.Nil(t, err)
	}

	return inputTestHandleChannelGeneralChirp{
		name:      name,
		params:    params,
		channelID: channelID,
		message:   m,
		hasError:  false,
		sockets:   sockets,
	}
}

func newFailTestHandleChannelGeneralChirp(t *testing.T, filename string, name string) inputTestHandleChannelGeneralChirp {
	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/social/chirps"

	file := filepath.Join(messageDataPath, filename)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	mockRepo := NewMockRepository(t)

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	require.NoError(t, err)
	mockRepo.On("GetServerPubKey").Return(senderBuf, nil).Maybe()

	params := newHandlerParameters(mockRepo)

	return inputTestHandleChannelGeneralChirp{
		name:      name,
		params:    params,
		channelID: channelID,
		message:   m,
		hasError:  true,
	}
}
