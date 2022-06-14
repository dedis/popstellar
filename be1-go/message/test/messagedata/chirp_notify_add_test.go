package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Chirp_Notify_Add(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_notify_add", "chirp_notify_add.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "notify_add", action)

	var msg messagedata.ChirpNotifyAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "notify_add", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, int64(1634760180), msg.Timestamp)
}

func Test_Chirp_Notify_Add_Interface_Functions(t *testing.T) {
	var msg messagedata.ChirpNotifyAdd

	require.Equal(t, messagedata.ChirpObject, msg.GetObject())
	require.Equal(t, messagedata.ChirpActionNotifyAdd, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Chirp_Notify_Add_Verify(t *testing.T) {
	var chirpNotifyAdd messagedata.ChirpNotifyAdd

	object, action := "chirp", "notify_add"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "chirp_notify_add", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &chirpNotifyAdd)
			require.NoError(t, err)

			err = chirpNotifyAdd.Verify()
			require.Error(t, err)
		}
	}

	t.Run("negative timestamp", getTestBadExample("wrong_chirp_notify_add_negative_time.json"))
	t.Run("chirp id not base64", getTestBadExample("wrong_chirp_notify_add_not_base_64_chirp_id.json"))
}
