package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_State(t *testing.T) {
	buf, err := testData.ReadFile("testdata/lao_state.json")
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "state", action)

	var msg LaoState

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "state", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ID)
	require.Equal(t, "LAO", msg.Name)
	require.Equal(t, int64(1633098234), msg.Creation)
	require.Equal(t, int64(1633099140), msg.LastModified)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", msg.Organizer)

	require.Len(t, msg.Witnesses, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.Witnesses[0])

	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ModificationID)

	require.Len(t, msg.ModificationSignatures, 1)
	require.Equal(t, "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", msg.ModificationSignatures[0].Witness)
	require.Equal(t, "XXX", msg.ModificationSignatures[0].Signature)
}

func Test_Lao_State_Interface_Functions(t *testing.T) {
	var msg LaoState

	require.Equal(t, messagedata.LAOObject, msg.GetObject())
	require.Equal(t, messagedata.LAOActionState, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
