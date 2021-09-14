package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Roll_Call_Close(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "roll_call_close.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "close", action)

	var msg messagedata.RollCallClose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "close", msg.Action)
	require.Equal(t, "XXX", msg.UpdateID)
	require.Equal(t, "XXX", msg.Closes)
	require.Equal(t, int64(123), msg.ClosedAt)
	require.Len(t, msg.Attendees, 1)
	require.Equal(t, "XXX", msg.Attendees[0])
}
