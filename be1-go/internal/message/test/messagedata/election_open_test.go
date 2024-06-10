package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/messagedata/election/melection"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Open(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_open", "election_open.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "open", action)

	var msg melection.ElectionOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "open", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, int64(1633099883), msg.OpenedAt)
}

func Test_Election_Open_Interface_Functions(t *testing.T) {
	var msg melection.ElectionOpen

	require.Equal(t, messagedata.ElectionObject, msg.GetObject())
	require.Equal(t, messagedata.ElectionActionOpen, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
