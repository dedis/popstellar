package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_Create(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "lao_create", "lao_create.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "create", action)

	var msg messagedata.LaoCreate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "create", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ID)
	require.Equal(t, "LAO", msg.Name)
	require.Equal(t, int64(1633098234), msg.Creation)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", msg.Organizer)

	require.Len(t, msg.Witnesses, 0)
	//require.Equal(t, "XXX", msg.Witnesses[0])

	err = msg.Verify()
	require.NoError(t, err)
}
