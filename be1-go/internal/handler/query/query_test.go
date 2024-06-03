package query

import (
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"os"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/validation"
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

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		isError  bool
		contains string
	}

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
			err := HandleQuery(&fakeSocket, arg.message)
			if arg.isError {
				require.Contains(t, err.Error(), arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}
