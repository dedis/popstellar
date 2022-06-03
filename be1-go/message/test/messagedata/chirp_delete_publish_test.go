package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Chirp_Delete_Publish(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_delete_publish", "chirp_delete_publish.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ChirpDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Chirp_Delete_Publish_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_delete_publish", "wrong_chirp_delete_publish_negative_time.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ChirpDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}

func Test_Chirp_Delete_Publish_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_delete_publish", "wrong_chirp_delete_publish_not_base_64_chirp_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "delete", action)

	var msg messagedata.ChirpDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "@@@", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}
