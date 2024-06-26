package hgreetserver

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"popstellar/internal/errors"
	mocks2 "popstellar/internal/handler/method/greetserver/hgreetserver/mocks"
	"popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_handleGreetServer(t *testing.T) {
	conf := mocks2.NewConfig(t)
	peers := mocks2.NewPeers(t)

	handler := New(conf, peers, zerolog.New(io.Discard))

	type input struct {
		name      string
		socket    *mocks.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	// Test 1: reply with greet server when receiving a greet server from a new server

	gs, gsBuf := generator.NewGreetServerQuery(t, "pk1", "ca1", "sa1")

	fakeSocket := mocks.NewFakeSocket("1")
	peers.On("AddPeerInfo", fakeSocket.Id, gs.Params).Return(nil)
	peers.On("IsPeerGreeted", fakeSocket.Id).Return(false)
	conf.On("GetServerInfo").Return("pk", "sk", "address", nil)
	peers.On("AddPeerGreeted", fakeSocket.Id)

	args = append(args, input{
		name:      "Test 1",
		socket:    fakeSocket,
		message:   gsBuf,
		needGreet: true,
		isError:   false,
	})

	// Test 2: doesn't reply with greet server when already greeted the server

	gs, gsBuf = generator.NewGreetServerQuery(t, "pk2", "ca2", "sa2")

	fakeSocket = mocks.NewFakeSocket("2")
	peers.On("AddPeerInfo", fakeSocket.Id, gs.Params).Return(nil)
	peers.On("IsPeerGreeted", fakeSocket.Id).Return(true)

	args = append(args, input{
		name:      "Test 2",
		message:   gsBuf,
		socket:    fakeSocket,
		needGreet: false,
		isError:   false,
	})

	// Test 3: return an error if the socket ID is already used by another server

	gs, gsBuf = generator.NewGreetServerQuery(t, "pk3", "ca3", "sa3")

	fakeSocket = mocks.NewFakeSocket("3")
	peers.On("AddPeerInfo", fakeSocket.Id, gs.Params).Return(errors.NewAccessDeniedError("Skt already used"))

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		message:  gsBuf,
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
