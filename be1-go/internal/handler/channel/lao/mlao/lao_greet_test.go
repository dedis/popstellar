package mlao

import (
	"embed"
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Lao_Greet(t *testing.T) {
	buf, err := testData.ReadFile("testdata/greeting.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "greet", action)

	var msg LaoGreet

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "greet", msg.Action)
	require.Equal(t, "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", msg.LaoID)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", msg.Frontend)
	require.Equal(t, "wss://popdemo.dedis.ch:8000/demo", msg.Address)
	require.Equal(t, 2, len(msg.Peers))
	require.Equal(t, "wss://popdemo.dedis.ch:8000/second-organizer-demo", msg.Peers[0].Address)
	require.Equal(t, "wss://popdemo.dedis.ch:8000/witness-demo", msg.Peers[1].Address)
}

func Test_Lao_Greet_Interface_Functions(t *testing.T) {
	var msg LaoGreet

	require.Equal(t, channel.LAOObject, msg.GetObject())
	require.Equal(t, channel.LAOActionGreet, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Greet_Getters(t *testing.T) {
	var msg LaoGreet

	require.Equal(t, "lao", msg.GetObject())
	require.Equal(t, "greet", msg.GetAction())
}
