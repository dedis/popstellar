package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
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

func Test_Reaction_Delete_Interface_Functions(t *testing.T) {
	var msg messagedata.ReactionDelete

	require.Equal(t, messagedata.ReactionObject, msg.GetObject())
	require.Equal(t, messagedata.ReactionActionDelete, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Reaction_Delete_Verify(t *testing.T) {
	var reactionDelete messagedata.ReactionDelete

	object, action := "reaction", "delete"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "reaction_delete", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &reactionDelete)
			require.NoError(t, err)

			err = reactionDelete.Verify()
			require.Error(t, err)
		}
	}

	t.Run("time is negative", getTestBadExample("wrong_reaction_delete_negative_time.json"))
	t.Run("reaction id is not base64", getTestBadExample("wrong_reaction_delete_not_base_64_reaction_id.json"))
}
