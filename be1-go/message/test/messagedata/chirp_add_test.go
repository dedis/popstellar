package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Chirp_Add(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_add_publish", "chirp_add_publish.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "chirp", object)
	require.Equal(t, "add", action)

	var msg messagedata.ChirpAdd

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "chirp", msg.Object)
	require.Equal(t, "add", msg.Action)
	require.Equal(t, "I love PoP", msg.Text)
	require.Equal(t, int64(1634760180), msg.Timestamp)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Chirp_Add_Interface_Functions(t *testing.T) {
	var msg messagedata.ChirpAdd

	require.Equal(t, messagedata.ChirpObject, msg.GetObject())
	require.Equal(t, messagedata.ChirpActionAdd, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Chirp_Add_Verify(t *testing.T) {
	var chirpAdd messagedata.ChirpAdd

	object, action := "chirp", "add"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "chirp_add_publish", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &chirpAdd)
			require.NoError(t, err)

			err = chirpAdd.Verify()
			require.Error(t, err)
		}
	}

	t.Run("timestamp is negative", getTestBadExample("wrong_chirp_add_publish_negative_time.json"))
}
