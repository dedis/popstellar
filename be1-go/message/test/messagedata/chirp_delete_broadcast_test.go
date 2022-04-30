package messagedata

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"
)

func Test_Chirp_Notify_Delete(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_notify_delete", "chirp_notify_delete.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "notify_delete", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "notify_delete", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Chirp_Notify_Delete_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_notify_delete", "wrong_chirp_notify_delete_negative_time.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "notify_delete", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "notify_delete", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}

func Test_Chirp_Notify_Delete_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_notify_delete", "wrong_chirp_notify_delete_not_base_64_chirp_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "notify_delete", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "notify_delete", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "@@@", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}
