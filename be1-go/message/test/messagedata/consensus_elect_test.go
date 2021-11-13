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

func Test_Consensus_Elect_Invalid_Instance(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_elect", "wrong_elect_invalid_instance.json")

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
	require.Equal(t, "GXVFZVHlVNpOJsdRsJkUmJW2hnrd9n_vKtEc7P6FMF4=", msg.InstanceID)
	require.Equal(t, int64(1634553005), msg.CreatedAt)

	require.Equal(t, "election", msg.Key.Type)
	require.Equal(t, "GXVFZVHlVNpOJsdRsJkUmJW2hnrd9n_vKtEc7P6FMF4=", msg.Key.ID)
	require.Equal(t, "state", msg.Key.Property)

	require.Equal(t, "started", msg.Value)

	err = msg.Verify()
	require.Error(t, err, "instance id is %s, should be %s", msg.InstanceID, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=")
}

func Test_Consensus_Elect_Not_Base64_Instance(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_elect", "wrong_elect_not_base_64_instance.json")

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
	require.Equal(t, "@@@", msg.InstanceID)
	require.Equal(t, int64(1634553005), msg.CreatedAt)

	require.Equal(t, "election", msg.Key.Type)
	require.Equal(t, "GXVFZVHlVNpOJsdRsJkUmJW2hnrd9n_vKtEc7P6FMF4=", msg.Key.ID)
	require.Equal(t, "state", msg.Key.Property)

	require.Equal(t, "started", msg.Value)

	err = msg.Verify()
	require.Error(t, err, "lao id is %s, should be base64URL encoded", msg.InstanceID)
}

func Test_Consensus_Elect_Negative_Created_At(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_elect", "wrong_elect_negative_created_at.json")

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
	require.Equal(t, int64(-1634553005), msg.CreatedAt)

	require.Equal(t, "election", msg.Key.Type)
	require.Equal(t, "GXVFZVHlVNpOJsdRsJkUmJW2hnrd9n_vKtEc7P6FMF4=", msg.Key.ID)
	require.Equal(t, "state", msg.Key.Property)

	require.Equal(t, "started", msg.Value)

	err = msg.Verify()
	require.Error(t, err, "consensus creation is %d, should be at minimum 0", msg.CreatedAt)
}
