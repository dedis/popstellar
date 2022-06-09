package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Meeting_State(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "meeting_state.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "meeting", object)
	require.Equal(t, "state", action)

	var msg messagedata.MeetingState

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
	var msg messagedata.MeetingState

	require.Equal(t, messagedata.MeetingObject, msg.GetObject())
	require.Equal(t, messagedata.MeetingActionState, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
