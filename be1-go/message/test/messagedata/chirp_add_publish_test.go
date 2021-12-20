package messagedata

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"
)

func Test_Chirp_Add_Publish(t *testing.T) {
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

func Test_Chirp_Add_Publish_Negative_Timestamp(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "chirp_add_publish", "wrong_chirp_add_publish_negative_time.json")

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
	require.Equal(t, int64(-1), msg.Timestamp)

	err = msg.Verify()
	require.Error(t, err)
}