package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_End(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_end", "election_end.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "end", action)

	var msg messagedata.ElectionEnd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "end", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, int64(1633099883), msg.CreatedAt)
	require.Equal(t, "tAUYpZDc7lOfrxyviK6V9UsezeubGUZR-TpwF52pzWU=", msg.RegisteredVotes)
}

func Test_Election_End_Interface_Functions(t *testing.T) {
	var msg messagedata.ElectionEnd

	require.Equal(t, messagedata.ElectionObject, msg.GetObject())
	require.Equal(t, messagedata.ElectionActionEnd, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
