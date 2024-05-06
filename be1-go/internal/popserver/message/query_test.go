package message

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"testing"
)

func Test_handleGreetServer(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	err = config.SetConfig(t, nil, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")
	require.NoError(t, err)

	type input struct {
		name      string
		socket    socket.FakeSocket
		message   []byte
		needGreet bool
		isError   bool
		contains  string
	}

	args := make([]input, 0)

	greetServer := generator.NewGreetServerQuery(t, "pk", "client", "server")

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

	err = peers.AddPeerInfo(fakeSocket.Id, method.GreetServerParams{})
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

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

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

	subs.AddChannel(channel)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewSubscribeQuery(t, ID, channel),
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
		message:  generator.NewSubscribeQuery(t, ID, channel),
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

func Test_handleUnsubscribe(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

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

	subs.AddChannel(channel)
	errAnswer := subs.Subscribe(channel, &fakeSocket)
	require.Nil(t, errAnswer)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		channel: channel,
		message: generator.NewUnsubscribeQuery(t, ID, channel),
		isError: false,
	})

	// Test 2: failed to unsubscribe because not subscribed to channel

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	subs.AddChannel(channel)

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		channel:  channel,
		message:  generator.NewUnsubscribeQuery(t, ID, channel),
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
		message:  generator.NewUnsubscribeQuery(t, ID, channel),
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
		message:  generator.NewUnsubscribeQuery(t, ID, channel),
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

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	type input struct {
		name     string
		socket   socket.FakeSocket
		ID       int
		message  []byte
		result   []message.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully catchup 4 messages on a channel

	fakeSocket := socket.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"
	messagesToCatchUp := []message.Message{
		generator.NewNothingMsg(t, "sender1", nil),
		generator.NewNothingMsg(t, "sender2", nil),
		generator.NewNothingMsg(t, "sender3", nil),
		generator.NewNothingMsg(t, "sender4", nil),
	}

	mockRepo.On("GetAllMessagesFromChannel", channel).Return(messagesToCatchUp, nil)

	args = append(args, input{
		name:    "Test 1",
		socket:  fakeSocket,
		ID:      ID,
		message: generator.NewCatchupQuery(t, ID, channel),
		result:  messagesToCatchUp,
		isError: false,
	})

	// Test 2: failed to catchup because DB is disconnected

	fakeSocket = socket.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	mockRepo.On("GetAllMessagesFromChannel", channel).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator.NewCatchupQuery(t, ID, channel),
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
				require.Equal(t, arg.result, arg.socket.Res)
			}
		})
	}
}

func Test_handleHeartbeat(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	mockRepository, err := database.SetDatabase(t)
	require.NoError(t, err)

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

	expected := make(map[string][]string)
	expected["/root"] = []string{
		msgIDs[1],
		msgIDs[2],
	}
	expected["root/lao1"] = []string{
		msgIDs[4],
	}

	mockRepository.On("GetParamsForGetMessageByID", heartbeatMsgIDs1).Return(expected, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		message:  generator.NewHeartbeatQuery(t, heartbeatMsgIDs1),
		expected: expected,
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
		message: generator.NewHeartbeatQuery(t, heartbeatMsgIDs2),
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
		name:     "failed to query DB",
		socket:   fakeSocket,
		message:  generator.NewHeartbeatQuery(t, heartbeatMsgIDs3),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, errAnswer := handleHeartbeat(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, errAnswer)
				require.Nil(t, id)
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

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	mockRepository, err := database.SetDatabase(t)
	require.NoError(t, err)

	type input struct {
		name        string
		message     []byte
		socket      *socket.FakeSocket
		result      map[string][]message.Message
		isErrorTest bool
	}

	msg := message.Message{
		Data:              "data",
		Sender:            "sender",
		Signature:         "signature",
		MessageID:         "messageID",
		WitnessSignatures: []message.WitnessSignature{},
	}

	paramsGetMessagesByID := make(map[string][]string)
	paramsGetMessagesByID["/root"] = []string{msg.MessageID}

	getMessagesByID := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},
		ID:     1,
		Params: paramsGetMessagesByID,
	}

	inputs := make([]input, 0)

	// get one message

	getMessagesByID2 := getMessagesByID
	paramsGetMessagesByID2 := make(map[string][]string)
	paramsGetMessagesByID2["/root2"] = []string{msg.MessageID}
	getMessagesByID2.Params = paramsGetMessagesByID2

	getMessagesByIDBuf, err := json.Marshal(&getMessagesByID2)
	require.NoError(t, err)

	result := make(map[string][]message.Message)
	result["/root"] = []message.Message{msg}

	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID2).Return(result, nil)

	s := &socket.FakeSocket{Id: "fakesocket"}

	inputs = append(inputs, input{
		name:        "get one message",
		message:     getMessagesByIDBuf,
		socket:      s,
		result:      result,
		isErrorTest: false,
	})

	// failed to query DB

	getMessagesByID3 := getMessagesByID
	paramsGetMessagesByID3 := make(map[string][]string)
	paramsGetMessagesByID3["/root3"] = []string{msg.MessageID}
	getMessagesByID3.Params = paramsGetMessagesByID3

	getMessagesByIDBuf, err = json.Marshal(&getMessagesByID3)
	require.NoError(t, err)

	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID3).Return(nil, xerrors.Errorf("DB is disconnected"))

	inputs = append(inputs, input{
		name:        "failed to query DB",
		message:     getMessagesByIDBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleGetMessagesByID(i.socket, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.NotNil(t, id)
			} else {
				require.Nil(t, errAnswer)
				require.Equal(t, i.result, i.socket.MissingMsgs)
			}
		})
	}
}
