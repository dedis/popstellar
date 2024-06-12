package mlao

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

func Test_Meeting_State(t *testing.T) {
	buf, err := testData.ReadFile("testdata/meeting_state.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "meeting", object)
	require.Equal(t, "state", action)

	var msg MeetingState

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "meeting", msg.Object)
	require.Equal(t, "state", msg.Action)
	require.Equal(t, "wY29dWimwUQa0EWerQ7bNsRddlYtHBgJiEL8ZHnzjv8=", msg.ID)
	require.Equal(t, "Meeting", msg.Name)
	require.Equal(t, int64(1633098331), msg.Creation)
	require.Equal(t, int64(1633098340), msg.LastModified)
	require.Equal(t, "EPFL", msg.Location)
	require.Equal(t, int64(1633098900), msg.Start)
	require.Equal(t, int64(1633102500), msg.End)
	require.Equal(t, "wY29dWimwUQa0EWerQ7bNsRddlYtHBgJiEL8ZHnzjv8=", msg.ModificationID)

	require.Len(t, msg.ModificationSignatures, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.ModificationSignatures[0].Witness)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Signature)
}

func Test_Meeting_State_Interface_Functions(t *testing.T) {
	var msg MeetingState

	require.Equal(t, channel.MeetingObject, msg.GetObject())
	require.Equal(t, channel.MeetingActionState, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
