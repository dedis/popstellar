package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
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
	require.Equal(t, "krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk=", msg.UpdateID)
	require.Equal(t, "fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U=", msg.Opens)
	require.Equal(t, int64(1633099127), msg.OpenedAt)
}

func Test_Roll_Call_Open_Interface_Functions(t *testing.T) {
	var msg messagedata.RollCallOpen

	require.Equal(t, messagedata.RollCallObject, msg.GetObject())
	require.Equal(t, messagedata.RollCallActionOpen, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
