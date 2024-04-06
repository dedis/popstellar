package message

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/state"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"testing"
)

func Test_handleCatchUp(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		socket      *popserver.FakeSocket
		result      []message.Message
		isErrorTest bool
	}

	msg := message.Message{
		Data:              "data",
		Sender:            "sender",
		Signature:         "signature",
		MessageID:         "messageID",
		WitnessSignatures: []message.WitnessSignature{},
	}

	catchup := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},
		ID: 1,
		Params: method.CatchupParams{
			Channel: "/root",
		},
	}

	inputs := make([]input, 0)

	// catch up three messages

	catchupBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	messagesToCatchUp := []message.Message{msg, msg, msg}

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(messagesToCatchUp, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	params := popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "catch up three messages",
		params:      params,
		message:     catchupBuf,
		socket:      s,
		result:      messagesToCatchUp,
		isErrorTest: false,
	})

	// failed to query DB

	catchupBuf, err = json.Marshal(&catchup)
	require.NoError(t, err)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(nil, xerrors.Errorf("DB is disconnected"))

	params = popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query DB",
		params:      params,
		message:     catchupBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleCatchUp(i.params, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.NotNil(t, id)
			} else {
				require.Nil(t, errAnswer)
				require.Equal(t, i.result, i.socket.Res)
			}
		})
	}
}

func Test_handleGetMessagesByID(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		socket      *popserver.FakeSocket
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

	getMessagesByIDBuf, err := json.Marshal(&getMessagesByID)
	require.NoError(t, err)

	result := make(map[string][]message.Message)
	result["/root"] = []message.Message{msg}

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID).Return(result, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	params := popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "get one message",
		params:      params,
		message:     getMessagesByIDBuf,
		socket:      s,
		result:      result,
		isErrorTest: false,
	})

	// failed to query DB

	getMessagesByIDBuf, err = json.Marshal(&getMessagesByID)
	require.NoError(t, err)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID).Return(nil, xerrors.Errorf("DB is disconnected"))

	params = popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query DB",
		params:      params,
		message:     getMessagesByIDBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleGetMessagesByID(i.params, i.message)
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

func Test_handleGreetServer(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		socket      *popserver.FakeSocket
		needSend    bool
		isErrorTest bool
	}

	serverInfo1 := method.GreetServerParams{
		PublicKey:     "pk1",
		ServerAddress: "srvAddr1",
		ClientAddress: "cltAddr1",
	}

	greetServer := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo1,
	}

	inputs := make([]input, 0)

	// reply with greetServer

	greetServerBuf, err := json.Marshal(&greetServer)
	require.NoError(t, err)

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return([]byte("publicKey"), nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	params := popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "reply with greetServer",
		params:      params,
		message:     greetServerBuf,
		socket:      s,
		needSend:    true,
		isErrorTest: false,
	})

	// do not reply with greetServer

	greetServerBuf, err = json.Marshal(&greetServer)
	require.NoError(t, err)

	s = &popserver.FakeSocket{Id: "fakesocket"}

	params = popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	params.Peers.AddPeerGreeted(s.Id)

	inputs = append(inputs, input{
		name:        "server already greeted",
		params:      params,
		message:     greetServerBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// Socket already used

	greetServerBuf, err = json.Marshal(&greetServer)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)
	err = params.Peers.AddPeerInfo(params.Socket.ID(), serverInfo1)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "Socket already used",
		params:      params,
		message:     greetServerBuf,
		isErrorTest: true,
	})

	// failed to query DB

	greetServerBuf, err = json.Marshal(&greetServer)
	require.NoError(t, err)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return(nil, xerrors.Errorf("DB is disconnected"))

	params = popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query DB",
		params:      params,
		message:     greetServerBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleGreetServer(i.params, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Nil(t, id)
			} else if i.needSend {
				require.Nil(t, errAnswer)
				require.NotNil(t, i.socket.Msg)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, i.socket.Msg)
			}
		})
	}
}

func Test_handleHeartbeat(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		socket      *popserver.FakeSocket
		needSend    bool
		isErrorTest bool
		expected    map[string][]string
	}

	msgIDs := []string{"msg0", "msg1", "msg2", "msg3", "msg4", "msg5", "msg6"}

	listMsg := make(map[string][]string)
	listMsg["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	listMsg["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
	}
	listMsg["root/lao2"] = []string{
		msgIDs[5],
		msgIDs[6],
	}

	heartbeat := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodHeartbeat,
		},
		Params: listMsg,
	}

	inputs := make([]input, 0)

	// reply with missingIDs

	heartbeatBuf, err := json.Marshal(&heartbeat)
	require.NoError(t, err)

	expected := make(map[string][]string)
	expected["/root"] = []string{
		msgIDs[1],
		msgIDs[2],
	}
	expected["root/lao1"] = []string{
		msgIDs[4],
	}

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(expected, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	params := popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "reply with missingIDs",
		params:      params,
		message:     heartbeatBuf,
		socket:      s,
		needSend:    true,
		isErrorTest: false,
		expected:    expected,
	})

	// already up to date

	heartbeatBuf, err = json.Marshal(&heartbeat)
	require.NoError(t, err)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(nil, nil)

	s = &popserver.FakeSocket{Id: "fakesocket"}

	params = popserver.NewHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "already up to date",
		params:      params,
		message:     heartbeatBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// failed to query DB

	heartbeatBuf, err = json.Marshal(&heartbeat)
	require.NoError(t, err)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(nil, xerrors.Errorf("DB is disconnected"))

	params = popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query DB",
		params:      params,
		message:     heartbeatBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleHeartbeat(i.params, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Nil(t, id)
			} else if i.needSend {
				require.Nil(t, errAnswer)
				require.NotNil(t, i.socket.Msg)

				var getMessageByID method.GetMessagesById
				err := json.Unmarshal(i.socket.Msg, &getMessageByID)
				require.NoError(t, err)

				require.Equal(t, i.expected, getMessageByID.Params)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, i.socket.Msg)
			}
		})
	}
}

func Test_handleSubscribe(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		isErrorTest bool
		subscribe   method.Subscribe
	}

	subscribe := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodSubscribe,
		},
		ID: 1,
		Params: method.SubscribeParams{
			Channel: "/root/lao1",
		},
	}

	inputs := make([]input, 0)

	// Subscribe to channel

	subscribeBuf, err := json.Marshal(&subscribe)
	require.NoError(t, err)

	params := popserver.NewHandlerParameters(nil)
	params.Subs.AddChannel("/root/lao1")

	inputs = append(inputs, input{
		name:        "Subscribe to channel",
		params:      params,
		message:     subscribeBuf,
		isErrorTest: false,
		subscribe:   subscribe,
	})

	// unknown channel

	subscribeBuf, err = json.Marshal(&subscribe)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "unknown channel",
		params:      params,
		message:     subscribeBuf,
		isErrorTest: true,
		subscribe:   subscribe,
	})

	// cannot Subscribe to root

	subscribeRoot := subscribe
	subscribeRoot.Params.Channel = "/root"

	subscribeRootBuf, err := json.Marshal(&subscribeRoot)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "cannot Subscribe to root",
		params:      params,
		message:     subscribeRootBuf,
		isErrorTest: true,
		subscribe:   subscribeRoot,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleSubscribe(i.params, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Equal(t, i.subscribe.ID, *id)
			} else {
				require.Nil(t, errAnswer)

				_, isSubscribed := i.params.Subs[i.subscribe.Params.Channel][i.params.Socket.ID()]
				require.True(t, isSubscribed)
			}
		})
	}
}

func Test_handleUnsubscribe(t *testing.T) {
	type input struct {
		name        string
		params      state.HandlerParameters
		message     []byte
		isErrorTest bool
		unsubscribe method.Unsubscribe
	}

	unsubscribe := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},
		ID: 1,
		Params: method.UnsubscribeParams{
			Channel: "/root/lao1",
		},
	}

	inputs := make([]input, 0)

	// Unsubscribe from channel

	unsubscribeBuf, err := json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params := popserver.NewHandlerParameters(nil)
	params.Subs["/root/lao1"] = make(map[string]socket.Socket)
	params.Subs["/root/lao1"][params.Socket.ID()] = params.Socket

	inputs = append(inputs, input{
		name:        "Unsubscribe from channel",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: false,
		unsubscribe: unsubscribe,
	})

	// cannot Unsubscribe without being subscribed

	unsubscribeBuf, err = json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)
	params.Subs["/root/lao1"] = make(map[string]socket.Socket)

	inputs = append(inputs, input{
		name:        "cannot Unsubscribe without being subscribed",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: true,
		unsubscribe: unsubscribe,
	})

	// unknown channel

	unsubscribeBuf, err = json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "unknown channel",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: true,
		unsubscribe: unsubscribe,
	})

	// cannot Unsubscribe from root

	unsubscribeRoot := unsubscribe
	unsubscribeRoot.Params.Channel = "/root"

	unsubscribeRootBuf, err := json.Marshal(&unsubscribeRoot)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "cannot Unsubscribe from root",
		params:      params,
		message:     unsubscribeRootBuf,
		isErrorTest: true,
		unsubscribe: unsubscribeRoot,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleUnsubscribe(i.params, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Equal(t, i.unsubscribe.ID, *id)
			} else {
				require.Nil(t, errAnswer)

				_, isUnsubscribed := i.params.Subs[i.unsubscribe.Params.Channel][i.params.Socket.ID()]
				require.False(t, isUnsubscribed)
			}
		})
	}
}
