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

func Test_Consensus_Elect_Accept_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusElectAccept

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionElectAccept, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Elect_Accept_Verify(t *testing.T) {
	var consensusElectAccept messagedata.ConsensusElectAccept

	object, action := "consensus", "elect_accept"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_elect_accept", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusElectAccept)
			require.NoError(t, err)

			err = consensusElectAccept.Verify()
			require.Error(t, err)
		}
	}

	t.Run("instance not base64", getTestBadExample("wrong_elect_accept_not_base_64_instance.json"))
	t.Run("message not base64", getTestBadExample("wrong_elect_accept_not_base_64_message.json"))
}
