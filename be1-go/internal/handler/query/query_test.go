package query

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/network/socket"
	"testing"
)

// nullMethodHandler is a struct that implements the MethodHandler interface with no-op methods
type nullMethodHandler struct{}

// Handle method for nullMethodHandler that always returns nil
func (n *nullMethodHandler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	return nil, nil
}

// Initialize methodHandlers with nullMethodHandler instances
var methodHandlers = MethodHandlers{
	catchup:         &nullMethodHandler{},
	getmessagesbyid: &nullMethodHandler{},
	greetserver:     &nullMethodHandler{},
	heartbeat:       &nullMethodHandler{},
	publish:         &nullMethodHandler{},
	subscribe:       &nullMethodHandler{},
	unsubscribe:     &nullMethodHandler{},
	rumor:           &nullMethodHandler{},
}

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		isError  bool
		contains string
	}

	handler := New(methodHandlers)

	args := make([]input, 0)

	// Test 1: failed to handled popquery because unknown method

	msg := generator.NewNothingQuery(t, 999)

	args = append(args, input{
		name:     "Test 1",
		message:  msg,
		isError:  true,
		contains: "unexpected method",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := mock.FakeSocket{Id: "fakesocket"}
			err := handler.Handle(&fakeSocket, arg.message)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}
