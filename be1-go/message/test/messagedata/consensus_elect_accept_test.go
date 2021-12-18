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
	file := filepath.Join(relativeExamplePath, "consensus_elect_accept", "elect_accept.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "elect_accept", action)

	var msg messagedata.ConsensusElectAccept

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "elect_accept", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.True(t, msg.Accept)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Elect_Accept_Not_Base64_Instance(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_elect_accept", "wrong_elect_accept_not_base_64_instance.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "elect_accept", action)

	var msg messagedata.ConsensusElectAccept

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "elect_accept", msg.Action)
	require.Equal(t, "@@@", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.True(t, msg.Accept)

	err = msg.Verify()
	require.Error(t, err, "instance id is %s, should be base64URL encoded", msg.InstanceID)
}

func Test_Consensus_Elect_Accept_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_elect_accept", "wrong_elect_accept_not_base_64_message.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "elect_accept", action)

	var msg messagedata.ConsensusElectAccept

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "elect_accept", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "@@@", msg.MessageID)
	require.True(t, msg.Accept)

	err = msg.Verify()
	require.Error(t, err, "message id is %s, should be base64URL encoded", msg.MessageID)
}
