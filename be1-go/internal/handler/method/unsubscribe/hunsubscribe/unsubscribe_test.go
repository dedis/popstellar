package hunsubscribe

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/errors"
	"popstellar/internal/generator"
	"popstellar/internal/handler/method/unsubscribe/hunsubscribe/mocks"
	mocks2 "popstellar/internal/network/socket/mocks"
	"testing"
)

func Test_handleUnsubscribe(t *testing.T) {
	subs := mocks.NewSubscribers(t)

	handler := New(subs)

	type input struct {
		name     string
		socket   *mocks2.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully unsubscribe from a subscribed channel

	fakeSocket := mocks2.NewFakeSocket("1")
	ID := 1
	channel := "/root/lao1"

	subs.On("Unsubscribe", channel, fakeSocket).Return(nil)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewUnsubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to unsubscribe because not subscribed to channel

	fakeSocket = mocks2.NewFakeSocket("2")
	ID = 2
	channel = "/root/lao2"

	subs.On("Unsubscribe", channel, fakeSocket).
		Return(errors.NewInvalidResourceError("cannot Unsubscribe from a channel not subscribed"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generator.NewUnsubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Unsubscribe from a channel not subscribed",
	})

	// Test 3: failed to unsubscribe because cannot unsubscribe from root channel

	fakeSocket = mocks2.NewFakeSocket("3")
	ID = 3
	channel = "/root"

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generator.NewUnsubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Unsubscribe from root channel",
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
