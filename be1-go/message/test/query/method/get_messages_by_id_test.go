package method

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message"
	"popstellar/message/query/method"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_GetMessagesById(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "get_messages_by_id", "get_messages_by_id.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, message.RPCTypeQuery, rpctype)

	var getMessagesById method.GetMessagesById

	err = json.Unmarshal(buf, &getMessagesById)
	require.NoError(t, err)

	require.Equal(t, "get_messages_by_id", getMessagesById.Method)
	require.Equal(t, "2.0", getMessagesById.JSONRPC)

	expected := make(map[string][]string)
	channel1 := "/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw="
	channel2 := "/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/HnXDyvSSron676Icmvcjk5zXvGLkPJ1fVOaWOxItzBE="
	idChannel1 := "DCBX48EuNO6q-Sr42ONqsj7opKiNeXyRzrjqTbZ_aMI="
	id1Channel2 := "z6SbjJ0Hw36k8L09-GVRq4PNmi06yQX4e8aZRSbUDwc="
	id2Channel2 := "txbTmVMwCDkZdoaAiEYfAKozVizZzkeMkeOlzq5qMlg="

	expected[channel1] = append(expected[channel1], (idChannel1))
	expected[channel2] = append(expected[channel2], id1Channel2, id2Channel2)

	require.Equal(t, expected, getMessagesById.Params)
}
