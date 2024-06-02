package query

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"testing"
)

func Test_handleSubscribe(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	type input struct {
		name     string
		socket   mocks.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully subscribe to a channel

	fakeSocket := mocks.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"

	errAnswer := subs.AddChannel(channel)
	require.Nil(t, errAnswer)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewSubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to subscribe to an unknown channel

	fakeSocket = mocks.FakeSocket{Id: "2"}
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

	fakeSocket = mocks.FakeSocket{Id: "3"}
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
			id, errAnswer := handleSubscribe(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
				require.Equal(t, arg.ID, *id)
			} else {
				require.Nil(t, errAnswer)
				isSubscribed, err := subs.IsSubscribed(arg.channel, &arg.socket)
				require.NoError(t, err)
				require.True(t, isSubscribed)
			}
		})
	}
}
