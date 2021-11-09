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
	file := filepath.Join(relativeExamplePath, "learn.json")

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

	require.Len(t, msg.Acceptors, 3)
	require.Equal(t, "pFFLRiOVyFX2UwFw8kd8PnVg6rshT-ofWYVAc_QuRz4=", msg.Acceptors[0])
	require.Equal(t, "cSaSHaZzvVR_sfcD5xngSxafK1eCDxmrd0d1C7-VHXJ=", msg.Acceptors[1])
	require.Equal(t, "OtP_nVgrshTofWYVAcQ-uRz44UD_2tFJUOLLvTbFmzO=", msg.Acceptors[2])
}
