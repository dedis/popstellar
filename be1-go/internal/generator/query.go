package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/message/mmessage"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"testing"
)

func NewGreetServerQuery(t *testing.T, publicKey, clientAddress, serverAddress string) (method.GreetServer, []byte) {
	serverInfo := method.GreetServerParams{
		PublicKey:     publicKey,
		ServerAddress: clientAddress,
		ClientAddress: serverAddress,
	}

	greetServer := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo,
	}

	greetServerBuf, err := json.Marshal(&greetServer)
	require.NoError(t, err)

	return greetServer, greetServerBuf
}

func NewSubscribeQuery(t *testing.T, queryID int, channel string) []byte {
	subscribe := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodSubscribe,
		},
		ID:     queryID,
		Params: method.SubscribeParams{Channel: channel},
	}

	subscribeBuf, err := json.Marshal(&subscribe)
	require.NoError(t, err)

	return subscribeBuf
}

func NewUnsubscribeQuery(t *testing.T, queryID int, channel string) []byte {
	unsubscribe := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},
		ID:     queryID,
		Params: method.UnsubscribeParams{Channel: channel},
	}

	unsubscribeBuf, err := json.Marshal(&unsubscribe)
	require.NoError(t, err)

	return unsubscribeBuf
}

func NewPublishQuery(t *testing.T, queryID int, channel string, msg mmessage.Message) []byte {
	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},
		ID: queryID,
		Params: method.PublishParams{
			Channel: channel,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	return publishBuf
}

func NewCatchupQuery(t *testing.T, queryID int, channel string) []byte {
	catchup := method.Catchup{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},
		ID:     queryID,
		Params: method.CatchupParams{Channel: channel},
	}

	catchupBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	return catchupBuf
}

func NewHeartbeatQuery(t *testing.T, msgIDsByChannel map[string][]string) []byte {
	heartbeat := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodHeartbeat,
		},
		Params: msgIDsByChannel,
	}

	heartbeatBuf, err := json.Marshal(&heartbeat)
	require.NoError(t, err)

	return heartbeatBuf
}

func NewGetMessagesByIDQuery(t *testing.T, queryID int, msgIDsByChannel map[string][]string) []byte {
	getMessagesByID := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGetMessagesById,
		},
		ID:     queryID,
		Params: msgIDsByChannel,
	}

	getMessagesByIDBuf, err := json.Marshal(&getMessagesByID)
	require.NoError(t, err)

	return getMessagesByIDBuf
}

func NewRumorQuery(t *testing.T, queryID int, senderID string, rumorID int, messages map[string][]mmessage.Message) []byte {
	rumor := method.Rumor{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodRumor,
		},
		ID: queryID,
		Params: method.ParamsRumor{
			SenderID: senderID,
			RumorID:  rumorID,
			Messages: messages,
		},
	}

	rumorBuf, err := json.Marshal(&rumor)
	require.NoError(t, err)

	return rumorBuf
}
