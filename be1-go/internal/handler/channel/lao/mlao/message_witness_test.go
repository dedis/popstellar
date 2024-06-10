package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Message_Witness(t *testing.T) {
	buf, err := testData.ReadFile("testdata/message_witness.json")
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "message", object)
	require.Equal(t, "witness", action)

	var msg MessageWitness

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "message", msg.Object)
	require.Equal(t, "witness", msg.Action)
	require.Equal(t, "kAG_m4nEQXkguuO_LVphXFE_c_dPoQrHNsb0MvwhXTA=", msg.MessageID)
	require.Equal(t, "Lgax5s25xVVF-6j5KNPE85oP3RyUtRZR0OSD5nNH34YT1DzlOFixmYyIcB5wZKjuKJ_nB3YkNwVGW5z96LC7Bw==", msg.Signature)
}

func Test_Message_Witness_Interface_Functions(t *testing.T) {
	var msg MessageWitness

	require.Equal(t, messagedata.MessageObject, msg.GetObject())
	require.Equal(t, messagedata.MessageActionWitness, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}
