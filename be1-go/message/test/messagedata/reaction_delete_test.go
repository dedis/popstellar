package messagedata

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"
)

func Test_Reaction_Delete(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_delete", "reaction_delete.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ReactionDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ReactionID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Reaction_Delete_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_delete", "wrong_reaction_delete_negative_time.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ReactionDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ReactionID)
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}

func Test_Chirp_Delete_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "reaction_delete", "wrong_reaction_delete_not_base_64_reaction_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ReactionDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "@@@", msg.ReactionID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}
