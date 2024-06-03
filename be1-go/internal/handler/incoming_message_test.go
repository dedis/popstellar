package handler

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/require"
	"os"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/validation"
	"testing"
)

func TestMain(m *testing.M) {
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	utils.InitUtils(schemaValidator)

	exitVal := m.Run()

	os.Exit(exitVal)
}

func Test_handleIncomingMessage(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

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
			fakeSocket := mock.FakeSocket{Id: "1"}
			err := HandleIncomingMessage(&fakeSocket, arg.message)
			require.Error(t, err, arg.contains)
		})
	}
}
