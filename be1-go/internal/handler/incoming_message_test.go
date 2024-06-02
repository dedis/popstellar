package handler

import (
	"encoding/base64"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"os"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/utils"
	generatortest2 "popstellar/internal/test/generatortest"
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
		message:  generatortest2.NewNothingQuery(t, 999),
		contains: "invalid json",
	})

	// Test 2: failed to handled popanswer because wrong publish popanswer format

	msg := generatortest2.NewNothingMsg(t, base64.URLEncoding.EncodeToString([]byte("sender")), nil)
	msg.MessageID = "wrong messageID"

	args = append(args, input{
		name:     "Test 2",
		message:  generatortest2.NewPublishQuery(t, 1, "/root/lao1", msg),
		contains: "invalid json",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "1"}
			err := HandleIncomingMessage(&fakeSocket, arg.message)
			require.Error(t, err)
			require.Contains(t, err.Error(), arg.contains)
		})
	}
}
