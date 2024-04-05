package hub

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"testing"
)

func Test_handleMessage(t *testing.T) {
	type input struct {
		name    string
		params  handlerParameters
		message []byte
	}

	inputs := make([]input, 0)

	// wrong json

	msg := struct {
		jsonrpc string `json:"jsonrpc"`
		id      string `json:"id"`
	}{
		jsonrpc: "2.0",
		id:      "999",
	}

	msgBuf, err := json.Marshal(msg)
	require.NoError(t, err)

	params := newHandlerParameters(nil)

	inputs = append(inputs, input{
		"wrong json",
		params,
		msgBuf,
	})

	// missing method

	msg2 := method.GreetServer{
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

	msgBuf2, err := json.Marshal(msg2)

	inputs = append(inputs, input{
		"missing method",
		params,
		msgBuf2,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			err := handleMessage(i.params, i.message)
			require.Error(t, err)
		})
	}
}
