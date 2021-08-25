package hub

import (
	"encoding/base64"
	"student20_pop/message"
	"testing"

	"github.com/stretchr/testify/require"
)

const messageID = "oJYBapM5ZuVrnggAwzQMa3oBLrFSjEQY-hv_JQRgs1U="

func TestInbox_AddWitnessSignature(t *testing.T) {
	inbox := createInbox("")

	msg, err := message.NewMessage(message.PublicKey{1, 2, 3}, message.Signature{1, 2, 3}, nil, nil)
	require.NoError(t, err)

	// Add a message to the inbox
	inbox.storeMessage(*msg)

	require.Equal(t, 1, len(inbox.msgs))

	buf, err := base64.URLEncoding.DecodeString(messageID)
	require.NoError(t, err)

	// Add the witness signature to the message in the inbox
	err = inbox.addWitnessSignature(buf[:], message.PublicKey{4, 5, 6}, message.Signature{7, 8, 9})
	require.NoError(t, err)

	// Check if the message was updated
	storedMsg, ok := inbox.getMessage(buf[:])
	require.True(t, ok)

	require.Equal(t, 1, len(storedMsg.WitnessSignatures))
}

func TestInbox_AddSigWrongMessages(t *testing.T) {
	inbox := createInbox("")

	buf, err := base64.URLEncoding.DecodeString(messageID)
	require.NoError(t, err)

	// Add the witness signature to the message in the inbox
	err = inbox.addWitnessSignature(buf[:], message.PublicKey{4, 5, 6}, message.Signature{7, 8, 9})
	require.Error(t, err)

	// Check that the message is still not in the inbox
	_, ok := inbox.getMessage(buf[:])
	require.False(t, ok)

	require.Equal(t, 0, len(inbox.msgs))
}

func TestInbox_AddWitnessSignatures(t *testing.T) {
	inbox := createInbox("")

	msg, err := message.NewMessage(message.PublicKey{1, 2, 3}, message.Signature{1, 2, 3}, nil, nil)
	require.NoError(t, err)

	// Add a message to the inbox
	inbox.storeMessage(*msg)

	require.Equal(t, 1, len(inbox.msgs))

	buf, err := base64.URLEncoding.DecodeString(messageID)
	require.NoError(t, err)

	signaturesNumber := 100
	for i := 0; i < signaturesNumber; i++ {
		// Add the witness signature to the message in the inbox
		err := inbox.addWitnessSignature(buf[:], message.PublicKey{byte(i)}, message.Signature{byte(i)})
		require.NoError(t, err)
	}

	// Check if the message was updated
	storedMsg, ok := inbox.getMessage(buf[:])
	require.True(t, ok)

	require.Equal(t, signaturesNumber, len(storedMsg.WitnessSignatures))
}
