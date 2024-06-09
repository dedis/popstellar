package greetserver

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/greetserver/mocks"
	"popstellar/internal/message/query/method"
	"popstellar/internal/mock"
	"testing"
)

//go:embed data_test/*.json
var dataTest embed.FS

func Test_handleGreetServer(t *testing.T) {
	conf := mocks.NewConfig(t)
	peers := mocks.NewPeers(t)

	handler := New(conf, peers)

	type input struct {
		name      string
		socket    *mock.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	greetServerBuf, err := dataTest.ReadFile("data_test/greet_server.json")
	require.NoError(t, err)

	var greetServer method.GreetServer
	err = json.Unmarshal(greetServerBuf, &greetServer)
	require.NoError(t, err)

	// Test 1: reply with greet server when receiving a greet server from a new server

	fakeSocket := mock.NewFakeSocket("1")
	peers.On("AddPeerInfo", fakeSocket.Id, greetServer.Params).Return(nil)
	peers.On("IsPeerGreeted", fakeSocket.Id).Return(false)
	conf.On("GetServerInfo").Return("pk", "sk", "address", nil)
	peers.On("AddPeerGreeted", fakeSocket.Id)

	args = append(args, input{
		name:      "Test 1",
		socket:    fakeSocket,
		message:   greetServerBuf,
		needGreet: true,
		isError:   false,
	})

	// Test 2: doesn't reply with greet server when already greeted the server

	fakeSocket = mock.NewFakeSocket("2")
	peers.On("AddPeerInfo", fakeSocket.Id, greetServer.Params).Return(nil)
	peers.On("IsPeerGreeted", fakeSocket.Id).Return(true)

	args = append(args, input{
		name:      "Test 2",
		message:   greetServerBuf,
		socket:    fakeSocket,
		needGreet: false,
		isError:   false,
	})

	// Test 3: return an error if the socket ID is already used by another server

	fakeSocket = mock.NewFakeSocket("3")
	peers.On("AddPeerInfo", fakeSocket.Id, greetServer.Params).Return(errors.NewAccessDeniedError("Skt already used"))

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		message:  greetServerBuf,
		isError:  true,
		contains: "Skt already used",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			_, err := handler.Handle(arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err)
				require.Contains(t, err.Error(), arg.contains)
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
