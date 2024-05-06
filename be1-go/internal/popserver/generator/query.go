package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
)

func NewGreetServerQuery(t *testing.T, publicKey, clientAddress, serverAddress string) []byte {
	serverInfo := method.GreetServerParams{
		PublicKey:     publicKey,
		ServerAddress: clientAddress,
		ClientAddress: serverAddress,
	}

	greetServer := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodGreetServer,
		},
		Params: serverInfo,
	}

	greetServerBuf, err := json.Marshal(&greetServer)
	require.NoError(t, err)

	return greetServerBuf
}

func NewSubscribeQuery(t *testing.T, queryID int, channel string) []byte {
	subscribe := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
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
			JSONRPCBase: jsonrpc.JSONRPCBase{
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

func NewPublishQuery(t *testing.T, queryID int, channel string, msg message.Message) []byte {
	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
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
			JSONRPCBase: jsonrpc.JSONRPCBase{
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
