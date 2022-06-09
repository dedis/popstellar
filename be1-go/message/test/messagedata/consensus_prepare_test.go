package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Prepare(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_prepare", "prepare.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "prepare", action)

	var msg messagedata.ConsensusPrepare

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "prepare", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760000), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.ProposedTry)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Prepare_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusPrepare

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionPrepare, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Prepare_Verify(t *testing.T) {
	var consensusPrepare messagedata.ConsensusPrepare

	object, action := "consensus", "prepare"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_prepare", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusPrepare)
			require.NoError(t, err)

			err = consensusPrepare.Verify()
			require.Error(t, err)
		}
	}

	t.Run("created at is negative", getTestBadExample("wrong_prepare_negative_created_at.json"))
	t.Run("proposed try is negative", getTestBadExample("wrong_prepare_negative_proposed_try.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_prepare_not_base_64_instance.json"))
	t.Run("message is not base64", getTestBadExample("wrong_prepare_not_base_64_message.json"))
}
