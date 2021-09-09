package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"student20_pop/message2/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Message_Witness(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "message_witness.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "message", object)
	require.Equal(t, "witness", action)

	var msg messagedata.MessageWitness

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "message", msg.Object)
	require.Equal(t, "witness", msg.Action)
	require.Equal(t, "XXX", msg.MessageID)
	require.Equal(t, "XXX", msg.Signature)
}
