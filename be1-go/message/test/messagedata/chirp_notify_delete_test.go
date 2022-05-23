package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Chirp_Notify_Delete(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_notify_delete", "chirp_notify_delete.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "notify_delete", action)

	var msg messagedata.ChirpNotifyDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "notify_delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, int64(1634760180), msg.Timestamp)
}

func Test_Chirp_Notify_Delete_Interface_Functions(t *testing.T) {
	var msg messagedata.ChirpNotifyDelete

	require.Equal(t, messagedata.ChirpObject, msg.GetObject())
	require.Equal(t, messagedata.ChirpActionNotifyDelete, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Chirp_Notify_Delete_Verify(t *testing.T) {

	var chirpNotifyDelete messagedata.ChirpNotifyDelete

	object, action := "chirp", "notify_delete"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "chirp_notify_delete", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &chirpNotifyDelete)
			require.NoError(t, err)

			err = chirpNotifyDelete.Verify()
			require.Error(t, err)
		}
	}

	t.Run("negative timestamp", getTestBadExample("wrong_chirp_notify_delete_negative_time.json"))
	t.Run("chirp id not base64", getTestBadExample("wrong_chirp_notify_delete_not_base_64_chirp_id.json"))
}
