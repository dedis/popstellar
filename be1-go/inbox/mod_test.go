package inbox

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"popstellar/message/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestInbox_AddWitnessSignature(t *testing.T) {
	inbox := NewInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add a message to the inbox
	inbox.StoreMessage(msg)
	require.Equal(t, 1, len(inbox.msgsMap))

	// Add the witness signature to the message in the inbox
	inbox.AddWitnessSignature(msg.MessageID, "456", "789")

	// Check if the message was updated
	storedMsg, ok := inbox.GetMessage(msg.MessageID)
	require.True(t, ok)
	require.Equal(t, 1, len(storedMsg.WitnessSignatures))
}

func TestInbox_AddWitnessSignature_MessageNotReceivedYet(t *testing.T) {
	inbox := NewInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add the witness signatures to a message that is not in the inbox yet
	inbox.AddWitnessSignature(msg.MessageID, "456", "789")
	inbox.AddWitnessSignature(msg.MessageID, "345", "678")

	// Check that the message is not in the inbox and that the signatures are pending
	_, ok := inbox.GetMessage(msg.MessageID)
	require.False(t, ok)
	require.Equal(t, 0, len(inbox.msgsMap))
	require.Equal(t, 2, len(inbox.pendingSignatures[msg.MessageID]))

	// Add the message to the inbox
	inbox.StoreMessage(msg)

	// Check if the message was added with all the signatures and that the pending signatures are removed
	storedMsg, ok := inbox.GetMessage(msg.MessageID)
	require.True(t, ok)
	require.Equal(t, 2, len(storedMsg.WitnessSignatures))
	require.Equal(t, 0, len(inbox.pendingSignatures[msg.MessageID]))
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
		inbox.AddWitnessSignature(msg.MessageID, fmt.Sprintf("%d", i), fmt.Sprintf("%d", i))
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
