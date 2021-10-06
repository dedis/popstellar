package lao

import (
	"fmt"
	"popstellar/channel"
	"popstellar/channel/generalChriping"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestBaseChannel_RollCallOrder(t *testing.T) {
	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}
	general := generalChriping.NewChannel("/root/XXX/social/posts/", fakeHubFunctionalities{})
	// Create the channel
	cha := NewChannel("channel0", fakeHubFunctionalities{}, messages[0], &general)

	laoChannel, ok := cha.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	for i := 1; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		laoChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := cha.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHubFunctionalities struct {
	channel.HubFunctionalities
}
