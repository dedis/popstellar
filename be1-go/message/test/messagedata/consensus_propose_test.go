package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Propose(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_propose", "propose.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "propose", action)

	var msg messagedata.ConsensusPropose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "propose", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760120), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.ProposedTry)
	require.True(t, msg.Value.ProposedValue)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Propose_Not_Base64_Instance(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_propose", "wrong_propose_not_base_64_instance.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "propose", action)

	var msg messagedata.ConsensusPropose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "propose", msg.Action)
	require.Equal(t, "@@@", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760120), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.ProposedTry)
	require.True(t, msg.Value.ProposedValue)

	err = msg.Verify()
	require.Error(t, err, "instance id is %s, should be base64URL encoded", msg.InstanceID)
}

func Test_Consensus_Propose_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_propose", "wrong_propose_not_base_64_message.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "propose", action)

	var msg messagedata.ConsensusPropose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "propose", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "@@@", msg.MessageID)
	require.Equal(t, int64(1634760120), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.ProposedTry)
	require.True(t, msg.Value.ProposedValue)

	err = msg.Verify()
	require.Error(t, err, "message id is %s, should be base64URL encoded", msg.MessageID)
}

func Test_Consensus_Propose_Negative_Created_At(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_propose", "wrong_propose_negative_created_at.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "propose", action)

	var msg messagedata.ConsensusPropose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "propose", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(-1634760120), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.ProposedTry)
	require.True(t, msg.Value.ProposedValue)

	err = msg.Verify()
	require.Error(t, err, "created at is %d, should be minimum 0", msg.CreatedAt)
}

func Test_Consensus_Propose_Negative_Proposed_Try(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_propose", "wrong_propose_negative_proposed_try.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "propose", action)

	var msg messagedata.ConsensusPropose

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "propose", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760120), msg.CreatedAt)
	require.Equal(t, int64(-4), msg.Value.ProposedTry)
	require.True(t, msg.Value.ProposedValue)

	err = msg.Verify()
	require.Error(t, err, "proposed try is %d, should be minimum 0", msg.CreatedAt)
}
