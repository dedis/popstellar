package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Roll_Call_Create(t *testing.T) {
	buf, err := testData.ReadFile("testdata/roll_call_create.json")
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "create", action)

	var msg RollCallCreate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "roll_call", msg.Object)
	require.Equal(t, "create", msg.Action)
	require.Equal(t, "fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U=", msg.ID)
	require.Equal(t, "Roll Call ", msg.Name)
	require.Equal(t, int64(1633098853), msg.Creation)
	require.Equal(t, int64(1633099125), msg.ProposedStart)
	require.Equal(t, int64(1633099140), msg.ProposedEnd)
	require.Equal(t, "EPFL", msg.Location)
	require.Equal(t, "Food is welcome!", msg.Description)
}

func Test_Roll_Call_Create_Interface_Functions(t *testing.T) {
	var msg RollCallCreate

	require.Equal(t, messagedata.RollCallObject, msg.GetObject())
	require.Equal(t, messagedata.RollCallActionCreate, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
