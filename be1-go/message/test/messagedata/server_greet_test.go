package messagedata

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"
)

func Test_Server_Greet(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "server_greet", "server_greet.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "server", object)
	require.Equal(t, "greet", action)

	var msg messagedata.ServerGreet

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "server", msg.Object)
	require.Equal(t, "greet", msg.Action)
	//public key modifier dans le json
	require.Equal(t, "public_key_hash", msg.PublicKey)
	require.Equal(t, "wss://popdemo.dedis.ch:9000/client", msg.ClientAddress)
	require.Equal(t, "wss://popdemo.dedis.ch:9001/server", msg.ServerAddress)
	require.Equal(t, 1, len(msg.Peers))
	require.Equal(t, "wss://popdemo.dedis.ch:9000/second-organizer-demo", msg.Peers[0].Address)
}

func Test_Server_Greet_Interface_Functions(t *testing.T) {
	var msg messagedata.ServerGreet

	require.Equal(t, messagedata.ServerObject, msg.GetObject())
	require.Equal(t, messagedata.ServerActionGreet, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Server_Greet_Getters(t *testing.T) {
	var msg messagedata.ServerGreet

	require.Equal(t, "server", msg.GetObject())
	require.Equal(t, "greet", msg.GetAction())
}
