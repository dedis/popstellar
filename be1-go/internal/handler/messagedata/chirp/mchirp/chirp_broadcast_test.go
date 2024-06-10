package mchirp

import (
	"encoding/json"
	"popstellar/internal/handler/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Chirp_Broadcast_Verify(t *testing.T) {
	var chirpBroadcast ChirpBroadcast

	object, action := "chirp", "notify_add"

	// Get valid example
	buf, err := testData.ReadFile("testdata/chirp_notify_add.json")
	require.NoError(t, err)

	err = json.Unmarshal(buf, &chirpBroadcast)
	require.NoError(t, err)

	// test valid example
	err = chirpBroadcast.Verify()
	require.NoError(t, err)

	// function to test bad examples
	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &chirpBroadcast)
			require.NoError(t, err)

			err = chirpBroadcast.Verify()
			require.Error(t, err)
		}
	}

	t.Run("negative timestamp", getTestBadExample("wrong_chirp_notify_add_negative_time.json"))
	t.Run("chirp id not base64", getTestBadExample("wrong_chirp_notify_add_not_base_64_chirp_id.json"))
}
