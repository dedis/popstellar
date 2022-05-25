package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Lao_Create(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "lao_create", "lao_create.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "lao", object)
	require.Equal(t, "create", action)

	var msg messagedata.LaoCreate

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "lao", msg.Object)
	require.Equal(t, "create", msg.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", msg.ID)
	require.Equal(t, "LAO", msg.Name)
	require.Equal(t, int64(1633098234), msg.Creation)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", msg.Organizer)

	require.Len(t, msg.Witnesses, 0)
	//require.Equal(t, "XXX", msg.Witnesses[0])

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Lao_Create_Interface_Functions(t *testing.T) {
	var msg messagedata.LaoCreate

	require.Equal(t, messagedata.LAOObject, msg.GetObject())
	require.Equal(t, messagedata.LAOActionCreate, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Lao_Create_Verify(t *testing.T) {
	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "lao_create", file))
			require.NoError(t, err)

			object, action, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, "lao", object)
			require.Equal(t, "create", action)

			var msg messagedata.LaoCreate

			err = json.Unmarshal(buf, &msg)
			require.NoError(t, err)

			// test the bad example
			err = msg.Verify()
			require.Error(t, err)
		}
	}

	t.Run("id invalid hash", getTestBadExample("bad_lao_create_id_invalid_hash.json"))
	t.Run("empty lao name", getTestBadExample("bad_lao_create_empty_name.json"))
	t.Run("creation negative", getTestBadExample("bad_lao_create_creation_negative.json"))
	t.Run("organizer id not base64", getTestBadExample("bad_lao_create_organizer_not_base64.json"))
	t.Run("witness id not base64", getTestBadExample("bad_lao_create_witness_not_base64.json"))
	t.Run("witness id not base64", getTestBadExample("bad_lao_create_witness_not_base64.json"))
	t.Run("id not base64", getTestBadExample("bad_lao_create_id_not_base_64.json"))
}
