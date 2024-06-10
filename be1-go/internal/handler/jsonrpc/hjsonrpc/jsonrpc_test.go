package hjsonrpc

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/generator"
	"popstellar/internal/network/socket"
	"popstellar/internal/network/socket/mocks"
	"popstellar/internal/validation"
	"testing"
)

type nullQueryHandler struct{}

func (n *nullQueryHandler) Handle(socket socket.Socket, msg []byte) error {
	return nil
}

type nullAnswerHandler struct{}

func (n *nullAnswerHandler) Handle(msg []byte) error {
	return nil
}

func Test_handleIncomingMessage(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	queryHandler := &nullQueryHandler{}
	answerHandler := &nullAnswerHandler{}

	handler := New(schema, queryHandler, answerHandler)

	args := make([]input, 0)

	// Test 1: failed to handled popanswer because wrong json

	args = append(args, input{
		name:     "Test 1",
		message:  generator.NewNothingQuery(t, 999),
		contains: "failed to validate schema:",
	})

	// Test 2: failed to handled popanswer because wrong publish popanswer format

	msg := generator.NewNothingMsg(t, base64.URLEncoding.EncodeToString([]byte("sender")), nil)
	msg.MessageID = "wrong messageID"

	args = append(args, input{
		name:     "Test 2",
		message:  generator.NewPublishQuery(t, 1, "/root/lao1", msg),
		contains: "failed to validate schema:",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := mocks.FakeSocket{Id: "1"}
			err := handler.Handle(&fakeSocket, arg.message)
			require.Error(t, err, arg.contains)
		})
	}
}
