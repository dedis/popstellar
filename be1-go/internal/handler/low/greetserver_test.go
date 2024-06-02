package low

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"testing"
)

func Test_handleGreetServer(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(nil, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	type input struct {
		name      string
		socket    mocks.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	greetServer := generator.NewGreetServerQuery(t, "pk", "client", "server")

	// Test 1: reply with greet server when receiving a greet server from a new server

	fakeSocket := mocks.FakeSocket{Id: "1"}

	args = append(args, input{
		name:      "Test 1",
		socket:    fakeSocket,
		message:   greetServer,
		needGreet: true,
		isError:   false,
	})

	// Test 2: doesn't reply with greet server when already greeted the server

	fakeSocket = mocks.FakeSocket{Id: "2"}

	peers.AddPeerGreeted(fakeSocket.Id)

	args = append(args, input{
		name:      "Test 2",
		message:   greetServer,
		socket:    fakeSocket,
		needGreet: false,
		isError:   false,
	})

	// Test 3: return an error if the socket ID is already used by another server

	fakeSocket = mocks.FakeSocket{Id: "3"}

	err := peers.AddPeerInfo(fakeSocket.Id, method.GreetServerParams{})
	require.NoError(t, err)

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		message:  greetServer,
		isError:  true,
		contains: "failed to add peer",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, errAnswer := handleGreetServer(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
				require.Nil(t, id)
			} else if arg.needGreet {
				require.Nil(t, errAnswer)
				require.NotNil(t, arg.socket.Msg)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, arg.socket.Msg)
			}
		})
	}
}
