package lao

import (
	"fmt"
	"student20_pop/channel"
	"student20_pop/message/query/method"
	"student20_pop/message/query/method/message"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestBaseChannel_RollCallOrder(t *testing.T) {
	// Create the messages
	numMessages := 10

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: fmt.Sprintf("%d", 0)}

	// Create the channel
	channel := NewChannel("channel0", fakeHubThing{}, messages[0])

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	for i := 1; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		laoChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique timestamp for each message
		time.Sleep(time.Microsecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID)
	}

}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHubThing struct {
	channel.HubThingTheChannelNeeds
}
