package hsubscribe

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/errors"
	"popstellar/internal/handler/hmethod/hsubscribe/mocks"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"testing"
)

func Test_handleSubscribe(t *testing.T) {
	subs := mocks.NewSubscribers(t)

	handler := New(subs)

	type input struct {
		name     string
		socket   *mock.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully subscribe to a channel

	fakeSocket := mock.NewFakeSocket("1")
	ID := 1
	channel := "/root/lao1"

	subs.On("Subscribe", channel, fakeSocket).Return(nil)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewSubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to subscribe to an unknown channel

	fakeSocket = mock.NewFakeSocket("2")
	ID = 2
	channel = "/root/lao2"

	subs.On("Subscribe", channel, fakeSocket).Return(errors.NewInvalidResourceError("cannot Subscribe to unknown channel"))

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

	fakeSocket = mock.NewFakeSocket("3")
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
			_, err := handler.Handle(arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err)
				require.Contains(t, err.Error(), arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}
