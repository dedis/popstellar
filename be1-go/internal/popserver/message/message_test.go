package message

import (
	"encoding/base64"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"os"
	"popstellar/internal/popserver/generator"
	"popstellar/internal/popserver/util"
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

	util.InitUtils(&noLog, schemaValidator)

	exitVal := m.Run()

	os.Exit(exitVal)
}

func Test_handleMessage(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

	args := make([]input, 0)

	// Test 1: failed to handled message because wrong json

	args = append(args, input{
		name:     "Test 1",
		message:  generator.NewNothingQuery(t, 999),
		contains: "invalid json",
	})

	// Test 2: failed to handled message because wrong publish message format

	msg := generator.NewNothingMsg(t, base64.URLEncoding.EncodeToString([]byte("sender")), nil)
	msg.MessageID = "wrong messageID"

	args = append(args, input{
		name:     "Test 2",
		message:  generator.NewPublishQuery(t, 1, "/root/lao1", msg),
		contains: "invalid json",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "1"}
			err := HandleMessage(&fakeSocket, arg.message)
			require.Error(t, err)
			require.Contains(t, err.Error(), arg.contains)
		})
	}
}

func Test_handleQuery(t *testing.T) {
	type input struct {
		name     string
		message  []byte
		contains string
	}

	args := make([]input, 0)

	// Test 1: failed to handled query because unknown method

	msg := generator.NewNothingQuery(t, 999)

	args = append(args, input{
		name:     "Test 1",
		message:  msg,
		contains: "unexpected method",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			fakeSocket := socket.FakeSocket{Id: "fakesocket"}
			_, errAnswer := handleQuery(&fakeSocket, arg.message)
			require.NotNil(t, errAnswer)
			require.Contains(t, errAnswer.Error(), arg.contains)
		})
	}
}
