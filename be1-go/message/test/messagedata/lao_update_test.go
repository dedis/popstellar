package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_Update(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "lao_update.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "update_properties", action)

	var msg messagedata.LaoUpdate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "update_properties", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ID)
	require.Equal(t, "LAO", msg.Name)
	require.Equal(t, int64(1633099140), msg.LastModified)

	require.Len(t, msg.Witnesses, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.Witnesses[0])
}
