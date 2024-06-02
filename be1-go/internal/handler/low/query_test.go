package low

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"testing"
)

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

	args := make([]input, 0)

	// Test 1: failed to handled popquery because unknown method

	msg := generator.NewNothingQuery(t, 999)

	args = append(args, input{
		name:     "Test 1",
		message:  msg,
		contains: "unexpected method",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := mocks.FakeSocket{Id: "fakesocket"}
			errAnswer := handleQuery(&fakeSocket, arg.message)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}
}
