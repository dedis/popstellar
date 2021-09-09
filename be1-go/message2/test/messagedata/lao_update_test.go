package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
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
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, 123, msg.LastModified)

	require.Len(t, msg.Witnesses, 1)
	require.Equal(t, "XXX", msg.Witnesses[0])
}
