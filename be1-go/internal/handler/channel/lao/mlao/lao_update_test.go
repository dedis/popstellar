package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_Update(t *testing.T) {
	buf, err := testData.ReadFile("testdata/lao_update.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "update_properties", action)

	var msg LaoUpdate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "update_properties", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ID)
	require.Equal(t, "LAO", msg.Name)
	require.Equal(t, int64(1633099140), msg.LastModified)

	require.Len(t, msg.Witnesses, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.Witnesses[0])

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Lao_Update_Interface_Functions(t *testing.T) {
	var msg LaoUpdate

	require.Equal(t, channel.LAOObject, msg.GetObject())
	require.Equal(t, channel.LAOActionUpdate, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Lao_Update_Verify(t *testing.T) {
	var laoUpdate LaoUpdate

	object, action := "lao", "update_properties"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			obj, act, err := channel.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &laoUpdate)
			require.NoError(t, err)

			err = laoUpdate.Verify()
			require.Error(t, err)
		}
	}

	t.Run("name is empty", getTestBadExample("bad_lao_update_empty_name.json"))
	t.Run("is is not base64", getTestBadExample("bad_lao_update_id_not_base64.json"))
	t.Run("witness is not base64", getTestBadExample("bad_lao_update_witness_not_base64.json"))
	t.Run("last modified is negative", getTestBadExample("bad_lao_update_negative_last_modified.json"))
}
