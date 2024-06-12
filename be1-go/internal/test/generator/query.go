package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/catchup/mcatchup"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	"popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/subscribe/msubscribe"
	"popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/handler/query/mquery"
	"testing"
)

func NewGreetServerQuery(t *testing.T, publicKey, clientAddress, serverAddress string) (mgreetserver.GreetServer, []byte) {
	serverInfo := mgreetserver.GreetServerParams{
		PublicKey:     publicKey,
		ServerAddress: clientAddress,
		ClientAddress: serverAddress,
	}

	greetServer := mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo,
	}

	greetServerBuf, err := json.Marshal(&greetServer)
	require.NoError(t, err)

	return greetServer, greetServerBuf
}

func NewSubscribeQuery(t *testing.T, queryID int, channel string) []byte {
	subscribe := msubscribe.Subscribe{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodSubscribe,
		},
		ID:     queryID,
		Params: msubscribe.SubscribeParams{Channel: channel},
	}

	subscribeBuf, err := json.Marshal(&subscribe)
	require.NoError(t, err)

	return subscribeBuf
}

func NewUnsubscribeQuery(t *testing.T, queryID int, channel string) []byte {
	unsubscribe := munsubscribe.Unsubscribe{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodUnsubscribe,
		},
		ID:     queryID,
		Params: munsubscribe.UnsubscribeParams{Channel: channel},
	}

	unsubscribeBuf, err := json.Marshal(&unsubscribe)
	require.NoError(t, err)

	return unsubscribeBuf
}

func NewPublishQuery(t *testing.T, queryID int, channel string, msg mmessage.Message) []byte {
	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},
		ID: queryID,
		Params: mpublish.PublishParams{
			Channel: channel,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	return publishBuf
}

func NewCatchupQuery(t *testing.T, queryID int, channel string) []byte {
	catchup := mcatchup.Catchup{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodCatchUp,
		},
		ID:     queryID,
		Params: mcatchup.CatchupParams{Channel: channel},
	}

	catchupBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	return catchupBuf
}

func NewHeartbeatQuery(t *testing.T, msgIDsByChannel map[string][]string) []byte {
	heartbeat := mheartbeat.Heartbeat{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodHeartbeat,
		},
		Params: msgIDsByChannel,
	}

	heartbeatBuf, err := json.Marshal(&heartbeat)
	require.NoError(t, err)

	return heartbeatBuf
}

func NewGetMessagesByIDQuery(t *testing.T, queryID int, msgIDsByChannel map[string][]string) []byte {
	getMessagesByID := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodGetMessagesById,
		},
		ID:     queryID,
		Params: msgIDsByChannel,
	}

	getMessagesByIDBuf, err := json.Marshal(&getMessagesByID)
	require.NoError(t, err)

	return getMessagesByIDBuf
}

func NewRumorQuery(t *testing.T, queryID int, senderID string, rumorID int, messages map[string][]mmessage.Message) []byte {
	rumor := mrumor.Rumor{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodRumor,
		},
		ID: queryID,
		Params: mrumor.ParamsRumor{
			SenderID: senderID,
			RumorID:  rumorID,
			Messages: messages,
		},
	}

	rumorBuf, err := json.Marshal(&rumor)
	require.NoError(t, err)

	return rumorBuf
}
