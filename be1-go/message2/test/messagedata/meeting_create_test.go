package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Meeting_Create(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "meeting_create.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "meeting", object)
	require.Equal(t, "create", action)

	var msg messagedata.MeetingCreate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "meeting", msg.Object)
	require.Equal(t, "create", msg.Action)
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, int64(123), msg.Creation)
	require.Equal(t, "XXX", msg.Location)
	require.Equal(t, int64(123), msg.Start)
	require.Equal(t, int64(123), msg.End)
}
