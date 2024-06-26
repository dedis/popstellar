package mchirp

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

func Test_Chirp_Delete(t *testing.T) {
	buf, err := testData.ReadFile("testdata/chirp_delete_publish.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "delete", action)

	var msg ChirpDelete

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "delete", msg.Action)
	require.Equal(t, "ONYYu9Q2kGdAVpfbGwdmgBPf4QBznjt-JQO2gGCL3iI=", msg.ChirpID)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Chirp_Delete_Interface_Functions(t *testing.T) {
	var msg ChirpDelete

	require.Equal(t, channel.ChirpObject, msg.GetObject())
	require.Equal(t, channel.ChirpActionDelete, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Chirp_Delete_Verify(t *testing.T) {
	var chirpDelete ChirpDelete

	object, action := "chirp", "delete"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			obj, act, err := channel.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &chirpDelete)
			require.NoError(t, err)

			err = chirpDelete.Verify()
			require.Error(t, err)
		}
	}

	t.Run("timestamp is negative", getTestBadExample("wrong_chirp_delete_publish_negative_time.json"))
	t.Run("chirp id not base64", getTestBadExample("wrong_chirp_delete_publish_not_base_64_chirp_id.json"))
}
