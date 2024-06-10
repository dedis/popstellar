package melection

import (
	"encoding/json"
	"popstellar/internal/handler/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Election_Key(t *testing.T) {
	buf, err := testData.ReadFile("testdata/election_key.json")
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "election", object)
	require.Equal(t, "key", action)

	var msg ElectionKey

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "election", msg.Object)
	require.Equal(t, "key", msg.Action)
	require.Equal(t, "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=", msg.Election)
	require.Equal(t, "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ", msg.Key)
}

func Test_Key_New_Empty(t *testing.T) {
	var msg ElectionKey

	require.Empty(t, msg.NewEmpty())
}
