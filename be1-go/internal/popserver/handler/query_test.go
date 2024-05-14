package handler

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/database/repository"
	"popstellar/internal/popserver/generatortest"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"testing"
)

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

	args := make([]input, 0)

	// Test 1: failed to handled popquery because unknown method

	msg := generatortest.NewNothingQuery(t, 999)

	args = append(args, input{
		name:     "Test 1",
		message:  msg,
		contains: "unexpected method",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "fakesocket"}
			errAnswer := handleQuery(&fakeSocket, arg.message)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}
}

func Test_handleGreetServer(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.SetConfig(nil, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	type input struct {
		name      string
		socket    socket.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	greetServer := generatortest.NewGreetServerQuery(t, "pk", "client", "server")

	// Test 1: reply with greet server when receiving a greet server from a new server

	fakeSocket := socket.FakeSocket{Id: "1"}

	args = append(args, input{
		name:      "Test 1",
		socket:    fakeSocket,
		message:   greetServer,
		needGreet: true,
		isError:   false,
	})

	// Test 2: doesn't reply with greet server when already greeted the server

	fakeSocket = socket.FakeSocket{Id: "2"}

	peers.AddPeerGreeted(fakeSocket.Id)

	args = append(args, input{
		name:      "Test 2",
		message:   greetServer,
		socket:    fakeSocket,
		needGreet: false,
		isError:   false,
	})

	// Test 3: return an error if the socket ID is already used by another server

	fakeSocket = socket.FakeSocket{Id: "3"}

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

func Test_handleSubscribe(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	type input struct {
		name     string
		socket   socket.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully subscribe to a channel

	fakeSocket := socket.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"

	errAnswer := subs.AddChannel(channel)
	require.Nil(t, errAnswer)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generatortest.NewSubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to subscribe to an unknown channel

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generatortest.NewSubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Subscribe to unknown channel",
	})

	// cannot Subscribe to root

	fakeSocket = socket.FakeSocket{Id: "3"}
	ID = 3
	channel = "/root"

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generatortest.NewSubscribeQuery(t, ID, channel),
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

func Test_handleUnsubscribe(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	type input struct {
		name     string
		socket   socket.FakeSocket
		ID       int
		channel  string
		message  []byte
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully unsubscribe from a subscribed channel

	fakeSocket := socket.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"

	errAnswer := subs.AddChannel(channel)
	require.Nil(t, errAnswer)

	errAnswer = subs.Subscribe(channel, &fakeSocket)
	require.Nil(t, errAnswer)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generatortest.NewUnsubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to unsubscribe because not subscribed to channel

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	errAnswer = subs.AddChannel(channel)
	require.Nil(t, errAnswer)

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generatortest.NewUnsubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Unsubscribe from a channel not subscribed",
	})

	// Test 3: failed to unsubscribe because unknown channel

	fakeSocket = socket.FakeSocket{Id: "3"}
	ID = 3
	channel = "/root/lao3"

	args = append(args, input{
		name:     "Test 3",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generatortest.NewUnsubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Unsubscribe from unknown channel",
	})

	// Test 3: failed to unsubscribe because cannot unsubscribe from root channel

	fakeSocket = socket.FakeSocket{Id: "4"}
	ID = 4
	channel = "/root"

	args = append(args, input{
		name:     "Test 4",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generatortest.NewUnsubscribeQuery(t, ID, channel),
		isError:  true,
		contains: "cannot Unsubscribe from root channel",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, errAnswer := handleUnsubscribe(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
				require.Equal(t, arg.ID, *id)
			} else {
				require.Nil(t, errAnswer)

				isSubscribe, err := subs.IsSubscribed(arg.channel, &arg.socket)
				require.NoError(t, err)
				require.False(t, isSubscribe)
			}
		})
	}
}

func Test_handleCatchUp(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	mockRepository := repository.NewMockRepository(t)
	database.SetDatabase(mockRepository)

	type input struct {
		name     string
		socket   socket.FakeSocket
		ID       int
		message  []byte
		expected []message.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully catchup 4 messages on a channel

	fakeSocket := socket.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"
	messagesToCatchUp := []message.Message{
		generatortest.NewNothingMsg(t, "sender1", nil),
		generatortest.NewNothingMsg(t, "sender2", nil),
		generatortest.NewNothingMsg(t, "sender3", nil),
		generatortest.NewNothingMsg(t, "sender4", nil),
	}

	mockRepository.On("GetAllMessagesFromChannel", channel).Return(messagesToCatchUp, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		ID:       ID,
		message:  generatortest.NewCatchupQuery(t, ID, channel),
		expected: messagesToCatchUp,
		isError:  false,
	})

	// Test 2: failed to catchup because DB is disconnected

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	mockRepository.On("GetAllMessagesFromChannel", channel).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generatortest.NewCatchupQuery(t, ID, channel),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, errAnswer := handleCatchUp(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Contains(t, errAnswer.Error(), arg.contains)
				require.NotNil(t, id)
				require.Equal(t, arg.ID, *id)
			} else {
				require.Nil(t, errAnswer)
				require.Equal(t, arg.expected, arg.socket.Res)
			}
		})
	}
}

func Test_handleHeartbeat(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	mockRepository := repository.NewMockRepository(t)
	database.SetDatabase(mockRepository)

	type input struct {
		name     string
		socket   socket.FakeSocket
		message  []byte
		expected map[string][]string
		isError  bool
		contains string
	}

	msgIDs := []string{"msg0", "msg1", "msg2", "msg3", "msg4", "msg5", "msg6"}

	args := make([]input, 0)

	// Test 1: successfully handled heartbeat with some messages to catching up

	fakeSocket := socket.FakeSocket{Id: "1"}

	heartbeatMsgIDs1 := make(map[string][]string)
	heartbeatMsgIDs1["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	heartbeatMsgIDs1["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
	}
	heartbeatMsgIDs1["root/lao2"] = []string{
		msgIDs[5],
		msgIDs[6],
	}

	expected1 := make(map[string][]string)
	expected1["/root"] = []string{
		msgIDs[1],
		msgIDs[2],
	}
	expected1["root/lao1"] = []string{
		msgIDs[4],
	}

	mockRepository.On("GetParamsForGetMessageByID", heartbeatMsgIDs1).Return(expected1, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		message:  generatortest.NewHeartbeatQuery(t, heartbeatMsgIDs1),
		expected: expected1,
		isError:  false,
	})

	// Test 2: successfully handled heartbeat with nothing to catching up

	fakeSocket = socket.FakeSocket{Id: "2"}

	heartbeatMsgIDs2 := make(map[string][]string)
	heartbeatMsgIDs2["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}

	mockRepository.On("GetParamsForGetMessageByID", heartbeatMsgIDs2).Return(nil, nil)

	args = append(args, input{
		name:    "Test 2",
		socket:  fakeSocket,
		message: generatortest.NewHeartbeatQuery(t, heartbeatMsgIDs2),
		isError: false,
	})

	// Test 3: failed to handled heartbeat because DB is disconnected

	fakeSocket = socket.FakeSocket{Id: "3"}

	heartbeatMsgIDs3 := make(map[string][]string)
	heartbeatMsgIDs3["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	heartbeatMsgIDs3["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
	}

	mockRepository.On("GetParamsForGetMessageByID", heartbeatMsgIDs3).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "failed to popquery DB",
		socket:   fakeSocket,
		message:  generatortest.NewHeartbeatQuery(t, heartbeatMsgIDs3),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleHeartbeat(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
			} else if arg.expected != nil {
				require.Nil(t, errAnswer)
				require.NotNil(t, arg.socket.Msg)

				var getMessageByID method.GetMessagesById
				err := json.Unmarshal(arg.socket.Msg, &getMessageByID)
				require.NoError(t, err)

				require.Equal(t, arg.expected, getMessageByID.Params)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, arg.socket.Msg)
			}
		})
	}
}

func Test_handleGetMessagesByID(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	state.SetState(subs, peers, queries)

	mockRepository := repository.NewMockRepository(t)
	database.SetDatabase(mockRepository)

	type input struct {
		name     string
		socket   socket.FakeSocket
		ID       int
		message  []byte
		expected map[string][]message.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully handled getMessagesByID and sent the result

	fakeSocket := socket.FakeSocket{Id: "1"}
	ID := 1

	expected1 := make(map[string][]message.Message)
	expected1["/root"] = []message.Message{
		generatortest.NewNothingMsg(t, "sender1", nil),
		generatortest.NewNothingMsg(t, "sender2", nil),
		generatortest.NewNothingMsg(t, "sender3", nil),
		generatortest.NewNothingMsg(t, "sender4", nil),
	}
	expected1["/root/lao1"] = []message.Message{
		generatortest.NewNothingMsg(t, "sender5", nil),
		generatortest.NewNothingMsg(t, "sender6", nil),
	}

	paramsGetMessagesByID1 := make(map[string][]string)
	for k, v := range expected1 {
		paramsGetMessagesByID1[k] = make([]string, 0)
		for _, w := range v {
			paramsGetMessagesByID1[k] = append(paramsGetMessagesByID1[k], w.MessageID)
		}
	}

	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID1).Return(expected1, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		ID:       ID,
		message:  generatortest.NewGetMessagesByIDQuery(t, ID, paramsGetMessagesByID1),
		expected: expected1,
		isError:  false,
	})

	// Test 2: failed to handled getMessagesByID because DB is disconnected

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2

	paramsGetMessagesByID2 := make(map[string][]string)

	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID2).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generatortest.NewGetMessagesByIDQuery(t, ID, paramsGetMessagesByID2),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, errAnswer := handleGetMessagesByID(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.NotNil(t, id)
				require.Contains(t, errAnswer.Error(), arg.contains)
				require.Equal(t, arg.ID, *id)
			} else {
				require.Nil(t, errAnswer)
				require.NotNil(t, arg.expected)
				require.Equal(t, arg.expected, arg.socket.MissingMsgs)
			}
		})
	}
}
