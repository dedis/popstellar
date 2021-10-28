package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Elect_Accept(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "elect_accept.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "elect-accept", action)

	var msg messagedata.ConsensusElectAccept

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "elect-accept", msg.Action)
	require.Equal(t, "6z1k9Eqet9-YAOdEE9NaIQMvw8_W_Fj-u2vRL4siIb0=", msg.MessageID)
	require.True(t, msg.Accept)
}
