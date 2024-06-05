package greetserver

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"popstellar/internal/crypto"
	"popstellar/internal/message/query/method"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/types"
	"testing"
)

func Test_handleGreetServer(t *testing.T) {
	ownerPubBuf, err := base64.URLEncoding.DecodeString("3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY=")
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(ownerPubBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	conf := types.CreateConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	peers := types.NewPeers()

	handler := New(conf, peers)

	type input struct {
		name      string
		socket    mock.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	greetServer := generator.NewGreetServerQuery(t, "pk", "client", "server")

	// Test 1: reply with greet server when receiving a greet server from a new server

	fakeSocket := mock.FakeSocket{Id: "1"}

	args = append(args, input{
		name:      "Test 1",
		socket:    fakeSocket,
		message:   greetServer,
		needGreet: true,
		isError:   false,
	})

	// Test 2: doesn't reply with greet server when already greeted the server

	fakeSocket = mock.FakeSocket{Id: "2"}

	peers.AddPeerGreeted(fakeSocket.Id)

	args = append(args, input{
		name:      "Test 2",
		message:   greetServer,
		socket:    fakeSocket,
		needGreet: false,
		isError:   false,
	})

	// Test 3: return an error if the socket ID is already used by another server

	fakeSocket = mock.FakeSocket{Id: "3"}

	err = peers.AddPeerInfo(fakeSocket.Id, method.GreetServerParams{})
	require.NoError(t, err)

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		message:  greetServer,
		isError:  true,
		contains: "cannot add",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err := handler.Handle(&arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else if arg.needGreet {
				require.NoError(t, err)
				require.NotNil(t, arg.socket.Msg)
			} else {
				require.NoError(t, err)
				require.Nil(t, arg.socket.Msg)
			}
		})
	}
}
