package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Roll_Call_Open(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "roll_call_open.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "open", action)

	var msg messagedata.RollCallOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "open", msg.Action)
	require.Equal(t, "XXX", msg.UpdateID)
	require.Equal(t, "XXX", msg.Opens)
	require.Equal(t, 123, msg.OpenedAt)
}
