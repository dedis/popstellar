package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message/messagedata"
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
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, int64(123), msg.Creation)
	require.Equal(t, "XXX", msg.Location)
	require.Equal(t, int64(123), msg.Start)
	require.Equal(t, int64(123), msg.End)
	require.Equal(t, "XXX", msg.ModificationID)

	require.Len(t, msg.ModificationSignatures, 1)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Witness)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Signature)
}
