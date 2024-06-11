package mreaction

import (
	"embed"
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Reaction_Add(t *testing.T) {
	buf, err := testData.ReadFile("testdata/reaction_add.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "reaction", object)
	require.Equal(t, "add", action)

	var msg ReactionAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "reaction", msg.Object)
	require.Equal(t, "add", msg.Action)
	require.Equal(t, "👍", msg.ReactionCodepoint)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Reaction_Add_Interface_Functions(t *testing.T) {
	var msg ReactionAdd

	require.Equal(t, channel.ReactionObject, msg.GetObject())
	require.Equal(t, channel.ReactionActionAdd, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Reaction_Add_Verify(t *testing.T) {
	var reactionAdd ReactionAdd

	object, action := "reaction", "add"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			obj, act, err := channel.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &reactionAdd)
			require.NoError(t, err)

			err = reactionAdd.Verify()
			require.Error(t, err)
		}
	}

	t.Run("time is negative", getTestBadExample("wrong_reaction_add_negative_time.json"))
	t.Run("chirp id is not base64", getTestBadExample("wrong_reaction_add_not_base_64_chirp_id.json"))
}
