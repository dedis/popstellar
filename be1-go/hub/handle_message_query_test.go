package hub

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/hub/mocks"
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
		params      handlerParameters
		message     []byte
		socket      *fakeSocket
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

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(messagesToCatchUp, nil)

	s := &fakeSocket{id: "fakesocket"}

	params := newHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "catch up three messages",
		params:      params,
		message:     catchupBuf,
		socket:      s,
		result:      messagesToCatchUp,
		isErrorTest: false,
	})

	// failed to query db

	catchupBuf, err = json.Marshal(&catchup)
	require.NoError(t, err)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(nil, xerrors.Errorf("db is disconnected"))

	params = newHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query db",
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
				require.Equal(t, i.result, i.socket.res)
			}
		})
	}
}

func Test_handleGetMessagesByID(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
		message     []byte
		socket      *fakeSocket
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

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID).Return(result, nil)

	s := &fakeSocket{id: "fakesocket"}

	params := newHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "get one message",
		params:      params,
		message:     getMessagesByIDBuf,
		socket:      s,
		result:      result,
		isErrorTest: false,
	})

	// failed to query db

	getMessagesByIDBuf, err = json.Marshal(&getMessagesByID)
	require.NoError(t, err)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID).Return(nil, xerrors.Errorf("db is disconnected"))

	params = newHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query db",
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
				require.Equal(t, i.result, i.socket.missingMsgs)
			}
		})
	}
}

func Test_handleGreetServer(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
		message     []byte
		socket      *fakeSocket
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

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("GetServerPubKey").Return([]byte("publicKey"), nil)

	s := &fakeSocket{id: "fakesocket"}

	params := newHandlerParametersWithFakeSocket(mockRepository, s)

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

	s = &fakeSocket{id: "fakesocket"}

	params = newHandlerParametersWithFakeSocket(mockRepository, s)

	params.peers.AddPeerGreeted(s.id)

	inputs = append(inputs, input{
		name:        "server already greeted",
		params:      params,
		message:     greetServerBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// socket already used

	greetServerBuf, err = json.Marshal(&greetServer)
	require.NoError(t, err)

	params = newHandlerParameters(nil)
	err = params.peers.AddPeerInfo(params.socket.ID(), serverInfo1)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "socket already used",
		params:      params,
		message:     greetServerBuf,
		isErrorTest: true,
	})

	// failed to query db

	greetServerBuf, err = json.Marshal(&greetServer)
	require.NoError(t, err)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetServerPubKey").Return(nil, xerrors.Errorf("db is disconnected"))

	params = newHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query db",
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
				require.NotNil(t, i.socket.msg)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, i.socket.msg)
			}
		})
	}
}

func Test_handleHeartbeat(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
		message     []byte
		socket      *fakeSocket
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

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(expected, nil)

	s := &fakeSocket{id: "fakesocket"}

	params := newHandlerParametersWithFakeSocket(mockRepository, s)

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

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(nil, nil)

	s = &fakeSocket{id: "fakesocket"}

	params = newHandlerParametersWithFakeSocket(mockRepository, s)

	inputs = append(inputs, input{
		name:        "already up to date",
		params:      params,
		message:     heartbeatBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// failed to query db

	heartbeatBuf, err = json.Marshal(&heartbeat)
	require.NoError(t, err)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(nil, xerrors.Errorf("db is disconnected"))

	params = newHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query db",
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
				require.NotNil(t, i.socket.msg)

				var getMessageByID method.GetMessagesById
				err := json.Unmarshal(i.socket.msg, &getMessageByID)
				require.NoError(t, err)

				require.Equal(t, i.expected, getMessageByID.Params)
			} else {
				require.Nil(t, errAnswer)
				require.Nil(t, i.socket.msg)
			}
		})
	}
}

func Test_handleSubscribe(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
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

	// subscribe to channel

	subscribeBuf, err := json.Marshal(&subscribe)
	require.NoError(t, err)

	params := newHandlerParameters(nil)
	params.subs.addChannel("/root/lao1")

	inputs = append(inputs, input{
		name:        "subscribe to channel",
		params:      params,
		message:     subscribeBuf,
		isErrorTest: false,
		subscribe:   subscribe,
	})

	// unknown channel

	subscribeBuf, err = json.Marshal(&subscribe)
	require.NoError(t, err)

	params = newHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "unknown channel",
		params:      params,
		message:     subscribeBuf,
		isErrorTest: true,
		subscribe:   subscribe,
	})

	// cannot subscribe to root

	subscribeRoot := subscribe
	subscribeRoot.Params.Channel = "/root"

	subscribeRootBuf, err := json.Marshal(&subscribeRoot)
	require.NoError(t, err)

	params = newHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "cannot subscribe to root",
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

				_, isSubscribed := i.params.subs[i.subscribe.Params.Channel][i.params.socket.ID()]
				require.True(t, isSubscribed)
			}
		})
	}
}

func Test_handleUnsubscribe(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
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

	// unsubscribe from channel

	unsubscribeBuf, err := json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params := newHandlerParameters(nil)
	params.subs["/root/lao1"] = make(map[string]socket.Socket)
	params.subs["/root/lao1"][params.socket.ID()] = params.socket

	inputs = append(inputs, input{
		name:        "unsubscribe from channel",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: false,
		unsubscribe: unsubscribe,
	})

	// cannot unsubscribe without being subscribed

	unsubscribeBuf, err = json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params = newHandlerParameters(nil)
	params.subs["/root/lao1"] = make(map[string]socket.Socket)

	inputs = append(inputs, input{
		name:        "cannot unsubscribe without being subscribed",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: true,
		unsubscribe: unsubscribe,
	})

	// unknown channel

	unsubscribeBuf, err = json.Marshal(&unsubscribe)
	require.NoError(t, err)

	params = newHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "unknown channel",
		params:      params,
		message:     unsubscribeBuf,
		isErrorTest: true,
		unsubscribe: unsubscribe,
	})

	// cannot unsubscribe from root

	unsubscribeRoot := unsubscribe
	unsubscribeRoot.Params.Channel = "/root"

	unsubscribeRootBuf, err := json.Marshal(&unsubscribeRoot)
	require.NoError(t, err)

	params = newHandlerParameters(nil)

	inputs = append(inputs, input{
		name:        "cannot unsubscribe from root",
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

				_, isUnsubscribed := i.params.subs[i.unsubscribe.Params.Channel][i.params.socket.ID()]
				require.False(t, isUnsubscribed)
			}
		})
	}
}
