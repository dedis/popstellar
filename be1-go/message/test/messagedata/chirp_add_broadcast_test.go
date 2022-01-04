package messagedata

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"
)

func Test_Chirp_Add_Notify(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_add_notify", "chirp_add_notify.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "add_notify", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "add_notify", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpId)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Chirp_Add_Notify_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_add_notify", "wrong_chirp_add_notify_negative_time.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "add_notify", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "add_notify", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpId)
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}

func Test_Chirp_Add_Notify_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_add_notify", "wrong_chirp_add_notify_not_base_64_chirp_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "add_notify", action)

	var msg messagedata.ChirpBroadcast

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "add_notify", msg.Action)
	require.Equal(t, "/root/<lao_id>/social/<sender>", msg.Channel)
	require.Equal(t, "@@@", msg.ChirpId)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}
