package hub

import (
	"bytes"
	"student20_pop/message"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestBaseChannel_RollCallOrder(t *testing.T) {
	log := zerolog.New(io.Discard)

	// Create the channel
	channel := createBaseChannel(nil, "Channel0", log)

	// Create the messages
	messageNumber := 10
	messages := make([]message.Message, 0, messageNumber)
	for i := 0; i < messageNumber; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: []byte{byte(i)}}
		messages = append(messages, message)

		// Store the message in the inbox
		channel.inbox.storeMessage(message)

		// Wait before storing a new message to be able to have an unique timestamp for each message
		time.Sleep(time.Microsecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(message.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in `catchupAnswer`
	for i := 0; i < messageNumber; i++ {
		require.Equal(t, bytes.Compare(messages[i].MessageID, catchupAnswer[i].MessageID), 0)
	}

}
