package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Vote_Write_In(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "vote_cast_write_in.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "cast_vote", action)

	var msg messagedata.VoteCastWriteIn

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "cast_vote", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.Lao)
	require.Equal(t, "QWTmcWMMMiUdWdZX7ib7GyqH6A5ifDYwPaMpKxIZm1k=", msg.Election)
	require.Equal(t, int64(1633098996), msg.CreatedAt)

	require.Len(t, msg.Votes, 1)
	require.Equal(t, "DtIsj7nQ0Y4iLJ4ETKv2D0uah7IYGyEVW7aCLFjaL0w=", msg.Votes[0].ID)
	require.Equal(t, "WBVsWJI-C5YkD0wdE4DxnLa0lJzjnHEd67XPFVB9v3g=", msg.Votes[0].Question)
	require.Equal(t, "Computer Science", msg.Votes[0].WriteIn)
}
