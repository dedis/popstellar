package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Key(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_key", "election_key.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "key", action)

	var msg messagedata.ElectionKey

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "key", msg.Action)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ", msg.Key)
}

func Test_Key_New_Empty(t *testing.T) {
	var msg messagedata.ElectionKey

	require.Empty(t, msg.NewEmpty())
}
