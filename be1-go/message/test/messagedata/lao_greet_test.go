package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_Greet(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "lao_greet", "greeting.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "greet", action)

	var msg messagedata.LaoGreet

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "greet", msg.Action)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", msg.Frontend)
	require.Equal(t, "wss://popdemo.dedis.ch/demo", msg.Address)
}

func Test_Lao_Greet_New_Empty(t *testing.T) {
	var msg messagedata.LaoGreet

	require.Empty(t, msg.NewEmpty())
}
