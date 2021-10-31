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
	require.Equal(t, "6z1k9Eqet9-YAOdEE9NaIQMvw8_W_Fj-u2vRL4siIb0=", msg.MessageID)

	require.Len(t, msg.AcceptorSignatures, 3)
	require.Equal(t, "pFFLRiOVyFX2UwFw8kd8PnVg6rshT-ofWYVAc_QuRz4=", msg.AcceptorSignatures[0])
	require.Equal(t, "cSaSHaZzvVR_sfcD5xngSxafK1eCDxmrd0d1C7-VHXJ=", msg.AcceptorSignatures[1])
	require.Equal(t, "OtP_nVgrshTofWYVAcQ-uRz44UD_2tFJUOLLvTbFmzO=", msg.AcceptorSignatures[2])
}
