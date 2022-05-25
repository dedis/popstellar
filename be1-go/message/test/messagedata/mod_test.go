package messagedata

import (
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

var relativeExamplePath string

func init() {
	relativeExamplePath = filepath.Join("..", "..", "..", "..", "protocol",
		"examples", "messageData")
}

func Test_GetObjectAndAction(t *testing.T) {
	testWrongExamples := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath,
				"test_messages", file))
			require.NoError(t, err)

			_, _, err = messagedata.GetObjectAndAction(buf)
			require.Error(t, err)
		}
	}

	t.Run("object is missing", testWrongExamples("wrong_message_no_object.json"))
	t.Run("action is missing", testWrongExamples("wrong_message_no_action.json"))
	t.Run("json is invalid", testWrongExamples("wrong_message_invalid_json.json"))
}

func Test_GetTime(t *testing.T) {
	testWrongExamples := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath,
				"test_messages", file))
			require.NoError(t, err)

			_, err = messagedata.GetTime(buf)
			require.Error(t, err)
		}
	}

	t.Run("timestamp is missing", testWrongExamples("wrong_message_no_action.json"))
	t.Run("json is invalid", testWrongExamples("wrong_message_invalid_json.json"))
}
