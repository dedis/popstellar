package mlao

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

func Test_Meeting_Create(t *testing.T) {
	buf, err := testData.ReadFile("testdata/meeting_create.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "meeting", object)
	require.Equal(t, "create", action)

	var msg MeetingCreate

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
	var msg MeetingCreate

	require.Equal(t, channel.MeetingObject, msg.GetObject())
	require.Equal(t, channel.MeetingActionCreate, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
