package inbox

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"popstellar/message/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

const messageID = "oJYBapM5ZuVrnggAwzQMa3oBLrFSjEQY-hv_JQRgs1U="

func TestInbox_AddWitnessSignature(t *testing.T) {
	inbox := NewInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add a message to the inbox
	inbox.StoreMessage(msg)

	require.Equal(t, 1, len(inbox.msgsMap))

	// Add the witness signature to the message in the inbox
	err := inbox.AddWitnessSignature(msg.MessageID, "456", "789")
	require.NoError(t, err)

	// Check if the message was updated
	storedMsg, ok := inbox.GetMessage(msg.MessageID)
	require.True(t, ok)

	require.Equal(t, 1, len(storedMsg.WitnessSignatures))
}

func TestInbox_AddSigWrongMessages(t *testing.T) {
	inbox := NewInbox("")

	buf, err := base64.URLEncoding.DecodeString(messageID)
	require.NoError(t, err)

	// Add the witness signature to the message in the inbox
	err = inbox.AddWitnessSignature(string(buf), "456", "789")
	require.Error(t, err)

	// Check that the message is still not in the inbox
	_, ok := inbox.GetMessage(string(buf))
	require.False(t, ok)

	require.Equal(t, 0, len(inbox.msgsMap))
}

func TestInbox_AddWitnessSignatures(t *testing.T) {
	inbox := NewInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add a message to the inbox
	inbox.StoreMessage(msg)

	require.Equal(t, 1, len(inbox.msgsMap))

	signaturesNumber := 100
	for i := 0; i < signaturesNumber; i++ {
		// Add the witness signature to the message in the inbox
		err := inbox.AddWitnessSignature(msg.MessageID, fmt.Sprintf("%d", i), fmt.Sprintf("%d", i))
		require.NoError(t, err)
	}

	// Check if the message was updated
	storedMsg, ok := inbox.GetMessage(msg.MessageID)
	require.True(t, ok)

	require.Equal(t, signaturesNumber, len(storedMsg.WitnessSignatures))
}

// -----------------------------------------------------------------------------
// Utility functions

func newMessage(t *testing.T, sender string, signature string,
	witnessSignatures []message.WitnessSignature, data string) message.Message {

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

	msg.MessageID = base64.URLEncoding.EncodeToString(h.Sum(nil))

	return msg
}
