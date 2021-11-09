package serverInbox

import (
	"crypto/sha256"
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Message_Storing(t *testing.T) {
	serverInbox := NewServerInbox()

	messageID := "oJYBapM5ZuVrnggAwzQMa3oBLrFSjEQY-hv_JQRgs1U="
	msg := newMessage(t, "123", "123", nil, "", messageID)

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},
		ID: 0,
		Params: struct {
			Channel string          "json:\"channel\""
			Message message.Message `json:"message"`
		}{
			Channel: "/root/chan",
			Message: msg,
		},
	}

	serverInbox.StoreMessage(publish)
	result, ok := serverInbox.GetMessage(messageID)
	require.True(t, ok)
	require.Equal(t, &publish, result)

	getAllExpected := make([]string, 1)
	buf, err := json.Marshal(publish)
	require.NoError(t, err)
	getAllExpected[0] = string(buf)

	getAllResult := serverInbox.GetSortedMessages()
	require.Equal(t, getAllExpected, getAllResult)
}

// -----------------------------------------------------------------------------
// Utility functions

func newMessage(t *testing.T, sender string, signature string,
	witnessSignatures []message.WitnessSignature, data string, id string) message.Message {

	msg := message.Message{
		Data:              data,
		Sender:            sender,
		Signature:         signature,
		WitnessSignatures: witnessSignatures,
	}

	// MessageID is H(data||signature) encoded as base64URL
	h := sha256.New()
	h.Write([]byte(data))
	h.Write([]byte(signature))

	msg.MessageID = id

	return msg
}
