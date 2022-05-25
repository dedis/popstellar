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

func Test_Consensus_Propose_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusPropose

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionPropose, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Propose_Verify(t *testing.T) {
	var consensusPropose messagedata.ConsensusPropose

	object, action := "consensus", "propose"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_propose", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusPropose)
			require.NoError(t, err)

			err = consensusPropose.Verify()
			require.Error(t, err)
		}
	}

	t.Run("created at is negative", getTestBadExample("wrong_propose_negative_created_at.json"))
	t.Run("proposed try is negative", getTestBadExample("wrong_propose_negative_proposed_try.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_propose_not_base_64_instance.json"))
	t.Run("message is not base64", getTestBadExample("wrong_propose_not_base_64_message.json"))
	t.Run("acceptor is not base64", getTestBadExample("wrong_propose_not_base_64_acceptor.json"))
}
