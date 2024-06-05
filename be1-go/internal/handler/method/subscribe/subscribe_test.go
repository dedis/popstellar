package subscribe

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/state"
	"testing"
)

func Test_handleSubscribe(t *testing.T) {
	subs := state.NewSubscribers()

	handler := New(subs)

	type input struct {
		name     string
		socket   mock.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully subscribe to a channel

	fakeSocket := mock.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"

	err := subs.AddChannel(channel)
	require.NoError(t, err)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewSubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to subscribe to an unknown channel

	fakeSocket = mock.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generator.NewSubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Subscribe to unknown channel",
	})

	// cannot Subscribe to root

	fakeSocket = mock.FakeSocket{Id: "3"}
	ID = 3
	channel = "/root"

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generator.NewSubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Subscribe to root channel",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, err := handler.Handle(&arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err, arg.contains)
				require.Equal(t, arg.ID, *id)
			} else {
				require.NoError(t, err)
				isSubscribed, err := subs.IsSubscribed(arg.channel, &arg.socket)
				require.NoError(t, err)
				require.True(t, isSubscribed)
			}
		})
	}
}
