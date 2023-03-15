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

func Test_Hash(t *testing.T) {
	data1 := []string{"abcd", "1234"}
	data2 := "test 😀"
	data3 := "🫡"
	data4 := []string{"text 🥰", "🏉", "more text🎃️", "♠️"}

	require.Equal(t, messagedata.Hash(data1...), "61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw=")
	require.Equal(t, messagedata.Hash(), "47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU=")
	require.Equal(t, messagedata.Hash(data2), "8BMmJjQMPhtD0QwVor1uVB3B_PyMMyIbIvaDHcOQnTg=")
	require.Equal(t, messagedata.Hash(data3), "ht7cQAkPdd6o-ZFVW6gTbt0gEIEUcr5FTDgOaeW8BOU=")
	require.Equal(t, messagedata.Hash(data4...), "wANKJFj9q_ncRKalYmK4yozUpet33JaFXVQEpMcHdfU=")

}
