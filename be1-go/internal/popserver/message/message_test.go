package message

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/types"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
)

func Test_handleMessage(t *testing.T) {
	type input struct {
		name    string
		params  types.HandlerParameters
		message []byte
	}

	inputs := make([]input, 0)

	// wrong json

	wrongJson := struct {
		jsonrpc string `json:"jsonrpc"`
		id      string `json:"id"`
	}{
		jsonrpc: "2.0",
		id:      "999",
	}

	wrongJsonBuf, err := json.Marshal(wrongJson)
	require.NoError(t, err)

	params := popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		"wrong json",
		params,
		wrongJsonBuf,
	})

	// wrong publish

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		ID: 1,
		Params: method.PublishParams{
			Channel: "/root",
			Message: message.Message{
				Data:              base64.URLEncoding.EncodeToString([]byte("wrong data")),
				Sender:            base64.URLEncoding.EncodeToString([]byte("wrong sender")),
				Signature:         base64.URLEncoding.EncodeToString([]byte("wrong signature")),
				MessageID:         base64.URLEncoding.EncodeToString([]byte("wrong messageID")),
				WitnessSignatures: make([]message.WitnessSignature, 0),
			},
		},
	}

	wrongPublish, err := json.Marshal(publish)
	require.NoError(t, err)

	params = popserver.NewHandlerParameters(nil)

	inputs = append(inputs, input{
		name:    "wrong publish",
		params:  params,
		message: wrongPublish,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			err := HandleMessage(i.params, i.message)
			require.Error(t, err)
		})
	}
}

func Test_handleQuery(t *testing.T) {
	type input struct {
		name    string
		params  types.HandlerParameters
		message []byte
	}

	inputs := make([]input, 0)

	// missing method

	msg := method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "",
		},
		Params: method.GreetServerParams{
			PublicKey:     "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
			ServerAddress: "wss://popdemo.dedis.ch:9000/client",
			ClientAddress: "wss://popdemo.dedis.ch:9001/server",
		},
	}

	params := popserver.NewHandlerParameters(nil)

	msgBuf, err := json.Marshal(msg)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:    "wrong method",
		params:  params,
		message: msgBuf,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			_, errAnswer := handleQuery(i.params, i.message)
			require.NotNil(t, errAnswer)
		})
	}
}