package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
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
	require.Equal(t, "wY29dWimwUQa0EWerQ7bNsRddlYtHBgJiEL8ZHnzjv8=", msg.ID)
	require.Equal(t, "Meeting", msg.Name)
	require.Equal(t, int64(1633098331), msg.Creation)
	require.Equal(t, "EPFL", msg.Location)
	require.Equal(t, int64(1633098900), msg.Start)
	require.Equal(t, int64(1633102500), msg.End)
}

func Test_Meeting_Create_Interface_Functions(t *testing.T) {
	var msg messagedata.MeetingCreate

	require.Equal(t, messagedata.MeetingObject, msg.GetObject())
	require.Equal(t, messagedata.MeetingActionCreate, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
