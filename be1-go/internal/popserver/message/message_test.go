package message

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"os"
	"popstellar/internal/popserver/utils"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"testing"
)

var noLog = zerolog.New(io.Discard)

func TestMain(m *testing.M) {
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	utils.InitUtils(&noLog, schemaValidator)

	exitVal := m.Run()

	os.Exit(exitVal)
}

func Test_handleMessage(t *testing.T) {
	type input struct {
		name    string
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

	inputs = append(inputs, input{
		"wrong json",
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

	inputs = append(inputs, input{
		name:    "wrong publish",
		message: wrongPublish,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "fakesocket"}
			err := HandleMessage(&fakeSocket, i.message)
			require.Error(t, err)
		})
	}
}

func Test_handleQuery(t *testing.T) {
	type input struct {
		name    string
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

	msgBuf, err := json.Marshal(msg)
	require.NoError(t, err)

	inputs = append(inputs, input{
		name:    "wrong method",
		message: msgBuf,
	})

	// run all tests

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "fakesocket"}
			_, errAnswer := handleQuery(&fakeSocket, i.message)
			require.NotNil(t, errAnswer)
		})
	}
}
