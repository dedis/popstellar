package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
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
