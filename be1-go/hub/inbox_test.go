package hub

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	messageX "student20_pop/message2/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

const messageID = "oJYBapM5ZuVrnggAwzQMa3oBLrFSjEQY-hv_JQRgs1U="

func TestInbox_AddWitnessSignature(t *testing.T) {
	inbox := createInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add a message to the inbox
	inbox.storeMessage(msg)

	require.Equal(t, 1, len(inbox.msgs))

	// Add the witness signature to the message in the inbox
	err := inbox.addWitnessSignature(msg.MessageID, "456", "789")
	require.NoError(t, err)

	// Check if the message was updated
	storedMsg, ok := inbox.getMessage(msg.MessageID)
	require.True(t, ok)

	require.Equal(t, 1, len(storedMsg.WitnessSignatures))
}

func TestInbox_AddSigWrongMessages(t *testing.T) {
	inbox := createInbox("")

	buf, err := base64.URLEncoding.DecodeString(messageID)
	require.NoError(t, err)

	// Add the witness signature to the message in the inbox
	err = inbox.addWitnessSignature(string(buf), "456", "789")
	require.Error(t, err)

	// Check that the message is still not in the inbox
	_, ok := inbox.getMessage(string(buf))
	require.False(t, ok)

	require.Equal(t, 0, len(inbox.msgs))
}

func TestInbox_AddWitnessSignatures(t *testing.T) {
	inbox := createInbox("")

	msg := newMessage(t, "123", "123", nil, "")

	// Add a message to the inbox
	inbox.storeMessage(msg)

	require.Equal(t, 1, len(inbox.msgs))

	signaturesNumber := 100
	for i := 0; i < signaturesNumber; i++ {
		// Add the witness signature to the message in the inbox
		err := inbox.addWitnessSignature(msg.MessageID, fmt.Sprintf("%d", i), fmt.Sprintf("%d", i))
		require.NoError(t, err)
	}

	// Check if the message was updated
	storedMsg, ok := inbox.getMessage(msg.MessageID)
	require.True(t, ok)

	require.Equal(t, signaturesNumber, len(storedMsg.WitnessSignatures))
}

func newMessage(t *testing.T, sender string, signature string, witnessSignatures []messageX.WitnessSignature, data string) messageX.Message {
	msg := messageX.Message{
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
