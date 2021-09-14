package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_State(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "lao_state.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "state", action)

	var msg messagedata.LaoState

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "state", msg.Action)
	require.Equal(t, "XXX", msg.ID)
	require.Equal(t, "XXX", msg.Name)
	require.Equal(t, int64(123), msg.Creation)
	require.Equal(t, int64(123), msg.LastModified)
	require.Equal(t, "XXX", msg.Organizer)

	require.Len(t, msg.Witnesses, 1)
	require.Equal(t, "XXX", msg.Witnesses[0])

	require.Equal(t, "XXX", msg.ModificationID)

	require.Len(t, msg.ModificationSignatures, 1)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Witness)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Signature)
}
