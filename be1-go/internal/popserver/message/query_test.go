package message

import (
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
)

func Test_handleCatchUp(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	type input struct {
		name        string
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

	inputs := make([]input, 0)

	// catch up three messages

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

	catchupBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	messagesToCatchUp := []message.Message{msg, msg, msg}

	mockRepo.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(messagesToCatchUp, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	inputs = append(inputs, input{
		name:        "catch up three messages",
		message:     catchupBuf,
		socket:      s,
		result:      messagesToCatchUp,
		isErrorTest: false,
	})

	// failed to query DB

	catchup2 := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},
		ID: 1,
		Params: method.CatchupParams{
			Channel: "/root2",
		},
	}

	catchupBuf, err = json.Marshal(&catchup2)
	require.NoError(t, err)

	mockRepo.On("GetAllMessagesFromChannel", catchup2.Params.Channel).Return(nil, xerrors.Errorf("DB is disconnected"))

	inputs = append(inputs, input{
		name:        "failed to query DB",
		message:     catchupBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleCatchUp(i.socket, i.message)
			if i.isErrorTest {
				fmt.Println(errAnswer)
				require.NotNil(t, errAnswer)
				require.NotNil(t, id)
			} else {
				fmt.Println(errAnswer)
				require.Nil(t, errAnswer)
				require.Equal(t, i.result, i.socket.Res)
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

	getMessagesByID2 := getMessagesByID
	paramsGetMessagesByID2 := make(map[string][]string)
	paramsGetMessagesByID2["/root2"] = []string{msg.MessageID}
	getMessagesByID2.Params = paramsGetMessagesByID2

	getMessagesByIDBuf, err := json.Marshal(&getMessagesByID2)
	require.NoError(t, err)

	result := make(map[string][]message.Message)
	result["/root"] = []message.Message{msg}

	mockRepository.On("GetResultForGetMessagesByID", paramsGetMessagesByID2).Return(result, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

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
		name        string
		message     []byte
		socket      *popserver.FakeSocket
		needSend    bool
		isErrorTest bool
	}

	inputs := make([]input, 0)

	// reply with greetServer

	serverInfo1 := method.GreetServerParams{
		PublicKey:     "pk1",
		ServerAddress: "srvAddr1",
		ClientAddress: "cltAddr1",
	}

	greetServer1 := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo1,
	}

	greetServerBuf, err := json.Marshal(&greetServer1)
	require.NoError(t, err)

	s := &popserver.FakeSocket{Id: "fakesocket1"}

	inputs = append(inputs, input{
		name:        "reply with greetServer",
		message:     greetServerBuf,
		socket:      s,
		needSend:    true,
		isErrorTest: false,
	})

	// do not reply with greetServer

	serverInfo2 := method.GreetServerParams{
		PublicKey:     "pk1",
		ServerAddress: "srvAddr1",
		ClientAddress: "cltAddr1",
	}

	greetServer2 := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo2,
	}

	greetServerBuf, err = json.Marshal(&greetServer2)
	require.NoError(t, err)

	s = &popserver.FakeSocket{Id: "fakesocket2"}

	peers.AddPeerGreeted(s.Id)

	inputs = append(inputs, input{
		name:        "server already greeted",
		message:     greetServerBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// Socket already used

	serverInfo4 := method.GreetServerParams{
		PublicKey:     "pk1",
		ServerAddress: "srvAddr1",
		ClientAddress: "cltAddr1",
	}

	greetServer4 := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo4,
	}

	greetServerBuf, err = json.Marshal(&greetServer4)
	require.NoError(t, err)

	s = &popserver.FakeSocket{Id: "fakesocket4"}

	err = peers.AddPeerInfo(s.Id, serverInfo4)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "Socket already used",
		socket:      s,
		message:     greetServerBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleGreetServer(i.socket, i.message)
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

	listMsg2 := make(map[string][]string)
	listMsg2["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}

	listMsg3 := make(map[string][]string)
	listMsg3["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	listMsg3["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
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

	mockRepository.On("GetParamsForGetMessageByID", listMsg).Return(expected, nil)

	s := &popserver.FakeSocket{Id: "fakesocket"}

	inputs = append(inputs, input{
		name:        "reply with missingIDs",
		message:     heartbeatBuf,
		socket:      s,
		needSend:    true,
		isErrorTest: false,
		expected:    expected,
	})

	// already up to date

	heartbeat2 := heartbeat
	heartbeat2.Params = listMsg2

	heartbeatBuf, err = json.Marshal(&heartbeat2)
	require.NoError(t, err)

	mockRepository.On("GetParamsForGetMessageByID", listMsg2).Return(nil, nil)

	s = &popserver.FakeSocket{Id: "fakesocket"}

	inputs = append(inputs, input{
		name:        "already up to date",
		message:     heartbeatBuf,
		socket:      s,
		needSend:    false,
		isErrorTest: false,
	})

	// failed to query DB

	heartbeat3 := heartbeat
	heartbeat3.Params = listMsg3

	heartbeatBuf, err = json.Marshal(&heartbeat3)
	require.NoError(t, err)

	mockRepository.On("GetParamsForGetMessageByID", listMsg3).Return(nil, xerrors.Errorf("DB is disconnected"))

	inputs = append(inputs, input{
		name:        "failed to query DB",
		message:     heartbeatBuf,
		isErrorTest: true,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleHeartbeat(i.socket, i.message)
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
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	type input struct {
		name        string
		socket      *popserver.FakeSocket
		message     []byte
		isErrorTest bool
		subscribe   method.Subscribe
	}

	inputs := make([]input, 0)

	// Subscribe to channel

	subscribe1 := method.Subscribe{
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

	subscribeBuf, err := json.Marshal(&subscribe1)
	require.NoError(t, err)

	fakeSocket := popserver.FakeSocket{Id: "fakesocket1"}

	subs.AddChannel("/root/lao1")

	inputs = append(inputs, input{
		name:        "Subscribe to channel",
		socket:      &fakeSocket,
		message:     subscribeBuf,
		isErrorTest: false,
		subscribe:   subscribe1,
	})

	// unknown channel

	subscribe2 := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodSubscribe,
		},
		ID: 1,
		Params: method.SubscribeParams{
			Channel: "/root/lao2",
		},
	}

	subscribeBuf, err = json.Marshal(&subscribe2)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "unknown channel",
		message:     subscribeBuf,
		isErrorTest: true,
		subscribe:   subscribe2,
	})

	// cannot Subscribe to root

	subscribe3 := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodSubscribe,
		},
		ID: 1,
		Params: method.SubscribeParams{
			Channel: "/root",
		},
	}

	subscribeRootBuf, err := json.Marshal(&subscribe3)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "cannot Subscribe to root",
		message:     subscribeRootBuf,
		isErrorTest: true,
		subscribe:   subscribe3,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleSubscribe(i.socket, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Equal(t, i.subscribe.ID, *id)
			} else {
				require.Nil(t, errAnswer)

				isSubscribed, err := subs.IsSubscribed(i.subscribe.Params.Channel, i.socket)
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
		name        string
		socket      *popserver.FakeSocket
		message     []byte
		isErrorTest bool
		unsubscribe method.Unsubscribe
	}

	inputs := make([]input, 0)

	// Unsubscribe from channel

	unsubscribe1 := method.Unsubscribe{
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

	unsubscribeBuf, err := json.Marshal(&unsubscribe1)
	require.NoError(t, err)

	fakeSocket := popserver.FakeSocket{Id: "fakesocket1"}
	subs.AddChannel("/root/lao1")
	errAnswer := subs.Subscribe("/root/lao1", &fakeSocket)
	require.Nil(t, errAnswer)

	inputs = append(inputs, input{
		name:        "Unsubscribe from channel",
		socket:      &fakeSocket,
		message:     unsubscribeBuf,
		isErrorTest: false,
		unsubscribe: unsubscribe1,
	})

	// cannot Unsubscribe without being subscribed

	unsubscribe2 := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},
		ID: 1,
		Params: method.UnsubscribeParams{
			Channel: "/root/lao2",
		},
	}

	s := &popserver.FakeSocket{Id: "fakesocket2"}

	unsubscribeBuf, err = json.Marshal(&unsubscribe2)
	require.NoError(t, err)

	subs.AddChannel("/root/lao2")

	inputs = append(inputs, input{
		name:        "cannot Unsubscribe without being subscribed",
		message:     unsubscribeBuf,
		socket:      s,
		isErrorTest: true,
		unsubscribe: unsubscribe2,
	})

	// unknown channel

	unsubscribe3 := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},
		ID: 1,
		Params: method.UnsubscribeParams{
			Channel: "/root/lao3",
		},
	}

	s = &popserver.FakeSocket{Id: "fakesocket3"}

	unsubscribeBuf, err = json.Marshal(&unsubscribe3)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "unknown channel",
		message:     unsubscribeBuf,
		socket:      s,
		isErrorTest: true,
		unsubscribe: unsubscribe1,
	})

	// cannot Unsubscribe from root

	unsubscribe4 := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},
		ID: 1,
		Params: method.UnsubscribeParams{
			Channel: "/root",
		},
	}

	s = &popserver.FakeSocket{Id: "fakesocket4"}

	unsubscribeRootBuf, err := json.Marshal(&unsubscribe4)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:        "cannot Unsubscribe from root",
		message:     unsubscribeRootBuf,
		socket:      s,
		isErrorTest: true,
		unsubscribe: unsubscribe4,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			id, errAnswer := handleUnsubscribe(i.socket, i.message)
			if i.isErrorTest {
				require.NotNil(t, errAnswer)
				require.Equal(t, i.unsubscribe.ID, *id)
			} else {
				require.Nil(t, errAnswer)

				isSubscribe, err := subs.IsSubscribed(i.unsubscribe.Params.Channel, i.socket)
				require.NoError(t, err)
				require.False(t, isSubscribe)
			}
		})
	}
}
