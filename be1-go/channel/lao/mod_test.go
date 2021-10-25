package lao

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
	"time"

	"github.com/rs/zerolog"

	"github.com/stretchr/testify/require"
)

func TestBaseChannel_RollCallOrder(t *testing.T) {
	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}

	// Create the channel
	channel := NewChannel("channel0", fakeHubFunctionalities{}, messages[0], nolog)

	laoChannel, ok := channel.(*Channel)
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
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

func Test_Verify_Functions(t *testing.T) {

	// Create the channel
	numMessages := 1

	messages := make([]message.Message, numMessages)

	channel := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHubFunctionalities{}, messages[0], nolog)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Get the JSON
	relativeExamplePath := filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData")
	file := filepath.Join(relativeExamplePath, "roll_call_open.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "open", action)

	var msg messagedata.RollCallOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// Test the function
	err = laoChannel.verifyMessageRollCallOpenID(msg)
	require.NoError(t, err)
}

// -----------------------------------------------------------------------------
// Utility functions

var nolog = zerolog.New(io.Discard)

type fakeHubFunctionalities struct {
	channel.HubFunctionalities
}
