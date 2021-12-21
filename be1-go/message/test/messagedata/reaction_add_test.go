package messagedata

import (
"encoding/json"
"github.com/stretchr/testify/require"
"os"
"path/filepath"
"popstellar/message/messagedata"
"testing"
)

func Test_Reaction_Add(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_add", "reaction_add.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "add", action)

	var msg messagedata.ReactionAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "add", msg.Action)
	require.Equal(t, "👍", msg.ReactionCodepoint)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpId)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Reaction_Add_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_add", "wrong_reaction_add_negative_time.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "add", action)

	var msg messagedata.ReactionAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "add", msg.Action)
	require.Equal(t, "👍", msg.ReactionCodepoint)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpId)
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}

func Test_Chirp_Add_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_add", "wrong_reaction_add_not_base_64_chirp_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "add", action)

	var msg messagedata.ReactionAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "add", msg.Action)
	require.Equal(t, "👍", msg.ReactionCodepoint)
	require.Equal(t, "@@@", msg.ChirpId)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}
