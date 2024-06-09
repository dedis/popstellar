package query

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/query/mocks"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"testing"
)

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		isError  bool
		contains string
	}

	methodHandler := mocks.NewMethodHandler(t)

	methodHandlers := MethodHandlers{
		Catchup:         methodHandler,
		GetMessagesbyid: methodHandler,
		Greetserver:     methodHandler,
		Heartbeat:       methodHandler,
		Publish:         methodHandler,
		Subscribe:       methodHandler,
		Unsubscribe:     methodHandler,
		Rumor:           methodHandler,
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
