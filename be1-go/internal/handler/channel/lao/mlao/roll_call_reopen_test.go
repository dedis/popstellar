package mlao

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

func Test_Roll_Call_ReOpen(t *testing.T) {
	buf, err := testData.ReadFile("testdata/roll_call_reopen.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "reopen", action)

	var msg RollCallReOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "reopen", msg.Action)
	require.Equal(t, "sgMsQ4EPPwKbHw3TsiCwkyH1JvilxPn0Y9iTEcbNMl4=", msg.UpdateID)
	require.Equal(t, "WxoPg4wLpmog0Q5eQewQ5AAD19RW-8-6aSZ2mGIJRO8=", msg.Opens)
	require.Equal(t, int64(1633099137), msg.OpenedAt)
}

func Test_Roll_Call_ReOpen_Interface_Functions(t *testing.T) {
	var msg RollCallReOpen

	require.Equal(t, channel.RollCallObject, msg.GetObject())
	require.Equal(t, channel.RollCallActionReOpen, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
