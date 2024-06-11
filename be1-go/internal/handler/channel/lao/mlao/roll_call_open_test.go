package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Roll_Call_Open(t *testing.T) {
	buf, err := testData.ReadFile("testdata/roll_call_open.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "open", action)

	var msg RollCallOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "open", msg.Action)
	require.Equal(t, "krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk=", msg.UpdateID)
	require.Equal(t, "fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U=", msg.Opens)
	require.Equal(t, int64(1633099127), msg.OpenedAt)
}

func Test_Roll_Call_Open_Interface_Functions(t *testing.T) {
	var msg RollCallOpen

	require.Equal(t, channel.RollCallObject, msg.GetObject())
	require.Equal(t, channel.RollCallActionOpen, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
