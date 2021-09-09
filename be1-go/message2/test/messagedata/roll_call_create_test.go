package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Roll_Call_Create(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "roll_call_create.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "create", action)

	var msg messagedata.RollCallCreate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "create", msg.Action)
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, 123, msg.Creation)
	require.Equal(t, 123, msg.ProposedStart)
	require.Equal(t, 123, msg.ProposedEnd)
	require.Equal(t, "XXX", msg.Location)
	require.Equal(t, "XXX", msg.Description)
}
