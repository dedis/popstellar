package mmessage

import (
	"embed"
	"encoding/base64"
	"popstellar/internal/handler/messagedata/election/melection"
	"popstellar/internal/handler/messagedata/root/mroot"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_UnmarshalData(t *testing.T) {
	messageDataBuf, err := testData.ReadFile("testdata/lao_create.json")
	require.NoError(t, err)

	laoCreate := mroot.LaoCreate{}
	electionSetup := melection.ElectionSetup{}

	msg := Message{
		Data: string(messageDataBuf),
	}

	err = msg.UnmarshalData(&laoCreate)
	require.Error(t, err)

	err = msg.UnmarshalData(&electionSetup)
	require.Error(t, err)

	messageData := base64.URLEncoding.EncodeToString(messageDataBuf)

	msg = Message{
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
