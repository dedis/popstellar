package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Learn(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_learn", "learn.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "learn", action)

	var msg messagedata.ConsensusLearn

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "learn", msg.Action)

	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)

	require.Equal(t, int64(1635624000), msg.CreatedAt)

	require.Equal(t, true, msg.Value.Decision)

	require.Len(t, msg.AcceptorSignatures, 3)
	require.Equal(t, "pFFLRiOVyFX2UwFw8kd8PnVg6rshT-ofWYVAc_QuRz4=", msg.AcceptorSignatures[0])
	require.Equal(t, "cSaSHaZzvVR_sfcD5xngSxafK1eCDxmrd0d1C7-VHXJ=", msg.AcceptorSignatures[1])
	require.Equal(t, "OtP_nVgrshTofWYVAcQ-uRz44UD_2tFJUOLLvTbFmzO=", msg.AcceptorSignatures[2])

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Learn_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusLearn

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionLearn, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Learn_Verify(t *testing.T) {
	var consensusLearn messagedata.ConsensusLearn

	object, action := "consensus", "learn"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_learn", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusLearn)
			require.NoError(t, err)

			err = consensusLearn.Verify()
			require.Error(t, err)
		}
	}

	t.Run("acceptor is not base64", getTestBadExample("wrong_learn_not_base_64_acceptor.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_learn_not_base_64_instance.json"))
	t.Run("message is not base64", getTestBadExample("wrong_learn_not_base_64_message.json"))
	t.Run("created at is negative", getTestBadExample("wrong_learn_negative_created_at.json"))
}
