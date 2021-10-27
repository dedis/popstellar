package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Elect(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "elect.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "elect", action)

	var msg messagedata.ConsensusElect

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "elect", msg.Action)
	require.Equal(t, "0bknjMZT1rVyyHJbRQBGl0CjCIxUnSmZcnI1XNIdCpM=", msg.InstanceID)
	require.Equal(t, int64(1634553005), msg.CreatedAt)

	require.Equal(t, "election", msg.Key.Type)
	require.Equal(t, "-t0xoQZa-ryiW18JnTjJHCsCNehFxuXOFOsfgKHHkj0=", msg.Key.ID)
	require.Equal(t, "state", msg.Key.Property)

	require.Equal(t, "started", msg.Value)

	err = msg.Verify()
	require.NoError(t, err)
}
