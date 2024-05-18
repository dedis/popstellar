package test

import (
	"encoding/base64"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_UnmarshalData(t *testing.T) {
	messageDataPath := filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData", "lao_create", "lao_create.json")

	messageDataBuf, err := os.ReadFile(messageDataPath)
	require.NoError(t, err)

	laoCreate := messagedata.LaoCreate{}
	electionSetup := messagedata.ElectionSetup{}

	msg := message.Message{
		Data: string(messageDataBuf),
	}

	err = msg.UnmarshalData(&laoCreate)
	require.Error(t, err)

	err = msg.UnmarshalData(&electionSetup)
	require.Error(t, err)

	messageData := base64.URLEncoding.EncodeToString(messageDataBuf)

	msg = message.Message{
		Data: messageData,
	}

	err = msg.UnmarshalData(&laoCreate)
	require.NoError(t, err)

	require.Equal(t, "lao", laoCreate.Object)
	require.Equal(t, "create", laoCreate.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", laoCreate.ID)
	require.Equal(t, "LAO", laoCreate.Name)
	require.Equal(t, int64(1633098234), laoCreate.Creation)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", laoCreate.Organizer)

	require.Len(t, laoCreate.Witnesses, 0)
}
