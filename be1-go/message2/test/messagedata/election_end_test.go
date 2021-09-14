package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_End(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "election_end.json")

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
	require.Equal(t, "XXX", msg.LAO)
	require.Equal(t, "XXX", msg.Election)
	require.Equal(t, int64(123), msg.CreatedAt)
	require.Equal(t, "XXX", msg.RegisteredVotes)
}
