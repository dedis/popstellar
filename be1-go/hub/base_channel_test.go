package hub

import (
	"fmt"
	"student20_pop/message2/query/method"
	messageX "student20_pop/message2/query/method/message"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestBaseChannel_RollCallOrder(t *testing.T) {
	// Create the channel
	channel := createBaseChannel(nil, "Channel0")

	// Create the messages
	messageNumber := 10
	messages := make([]messageX.Message, 0, messageNumber)
	for i := 0; i < messageNumber; i++ {
		// Create a new message containing only an id
		message := messageX.Message{MessageID: fmt.Sprintf("%d", i)}
		messages = append(messages, message)

		// Store the message in the inbox
		channel.inbox.storeMessage(message)

		// Wait before storing a new message to be able to have an unique timestamp for each message
		time.Sleep(time.Microsecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in `catchupAnswer`
	for i := 0; i < messageNumber; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID, 0)
	}

}
