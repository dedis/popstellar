package channel

import (
	"embed"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_GetObjectAndAction(t *testing.T) {
	testWrongExamples := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			_, _, err = GetObjectAndAction(buf)
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
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			_, err = GetTime(buf)
			require.Error(t, err)
		}
	}

	t.Run("timestamp is missing", testWrongExamples("wrong_message_no_action.json"))
	t.Run("json is invalid", testWrongExamples("wrong_message_invalid_json.json"))
}

// tests the correctness of the hash function with examples
func Test_Hash(t *testing.T) {

	//examples have been taken from unit tests from the Scala system
	data1 := []string{"abcd", "1234"}
	data2 := "test 😀"
	data3 := "🫡"
	data4 := []string{"text 🥰", "🏉", "more text🎃️", "♠️"}

	// the expected hash has been taken from the Scala system
	require.Equal(t, Hash(data1...), "61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw=")
	require.Equal(t, Hash(), "47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU=")
	require.Equal(t, Hash(data2), "8BMmJjQMPhtD0QwVor1uVB3B_PyMMyIbIvaDHcOQnTg=")
	require.Equal(t, Hash(data3), "ht7cQAkPdd6o-ZFVW6gTbt0gEIEUcr5FTDgOaeW8BOU=")
	require.Equal(t, Hash(data4...), "wANKJFj9q_ncRKalYmK4yozUpet33JaFXVQEpMcHdfU=")

}
