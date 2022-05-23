package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
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
	require.Equal(t, "WxoPg4wLpmog0Q5eQewQ5AAD19RW-8-6aSZ2mGIJRO8=", msg.UpdateID)
	require.Equal(t, "krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk=", msg.Closes)
	require.Equal(t, int64(1633099135), msg.ClosedAt)
	require.Len(t, msg.Attendees, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.Attendees[0])
}

func Test_Roll_Call_Close_Interface_Functions(t *testing.T) {
	var msg messagedata.RollCallClose

	require.Equal(t, messagedata.RollCallObject, msg.GetObject())
	require.Equal(t, messagedata.RollCallActionClose, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
