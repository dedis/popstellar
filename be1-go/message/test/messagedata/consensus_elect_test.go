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
	file := filepath.Join(relativeExamplePath, "consensus_elect", "elect.json")

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
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, int64(1634553005), msg.CreatedAt)

	require.Equal(t, "election", msg.Key.Type)
	require.Equal(t, "GXVFZVHlVNpOJsdRsJkUmJW2hnrd9n_vKtEc7P6FMF4=", msg.Key.ID)
	require.Equal(t, "state", msg.Key.Property)

	require.Equal(t, "started", msg.Value)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Elect_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusElect

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionElect, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Elect_Verify(t *testing.T) {
	var consensusElect messagedata.ConsensusElect

	object, action := "consensus", "elect"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_elect", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusElect)
			require.NoError(t, err)

			err = consensusElect.Verify()
			require.Error(t, err)
		}
	}

	t.Run("created at is negative", getTestBadExample("wrong_elect_negative_created_at.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_elect_not_base_64_instance.json"))
	t.Run("instance id not wrong", getTestBadExample("wrong_elect_invalid_instance.json"))
}
