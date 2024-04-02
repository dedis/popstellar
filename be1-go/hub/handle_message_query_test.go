package hub

import (
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/hub/mocks"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
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
				fmt.Println(i.socket.missingMsgs)
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
