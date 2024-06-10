package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata/election/melection"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_End(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_end", "election_end.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := mmessage.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "end", action)

	var msg melection.ElectionEnd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "end", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, int64(1633099883), msg.CreatedAt)
	require.Equal(t, "GX9slST3yY_Mltkjimp-eNq71mfbSbQ9sruABYN8EoM=", msg.RegisteredVotes)
}

func Test_Election_End_Interface_Functions(t *testing.T) {
	var msg melection.ElectionEnd

	require.Equal(t, mmessage.ElectionObject, msg.GetObject())
	require.Equal(t, mmessage.ElectionActionEnd, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
