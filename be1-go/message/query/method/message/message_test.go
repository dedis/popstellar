package message

import (
	"encoding/base64"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_UnmarshalData(t *testing.T) {
	messageDataPath := filepath.Join("..", "..", "..", "..", "..", "protocol",
		"examples", "messageData", "lao_create.json")

	messageDataBuf, err := os.ReadFile(messageDataPath)
	require.NoError(t, err)

	messageData := base64.URLEncoding.EncodeToString(messageDataBuf)

	message := Message{
		Data: messageData,
	}

	// > I should be able to decode this message into a LaoCreate datamessage

	laoCreate := messagedata.LaoCreate{}

	err = message.UnmarshalData(&laoCreate)
	require.NoError(t, err)

	require.Equal(t, "lao", laoCreate.Object)
	require.Equal(t, "create", laoCreate.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", laoCreate.ID)
	require.Equal(t, "LAO", laoCreate.Name)
	require.Equal(t, int64(1633098234), laoCreate.Creation)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", laoCreate.Organizer)

	require.Len(t, laoCreate.Witnesses, 0)
	//require.Equal(t, "XXX", laoCreate.Witnesses[0])
}
