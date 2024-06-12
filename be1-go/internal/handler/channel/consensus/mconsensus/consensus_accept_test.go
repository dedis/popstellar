package mconsensus

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/channel"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Consensus_Accept(t *testing.T) {
	buf, err := testData.ReadFile("testdata/accept.json")
	require.NoError(t, err)

	object, action, err := channel.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "accept", action)

	var msg ConsensusAccept

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "accept", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760180), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Accept_Interface_Functions(t *testing.T) {
	var msg ConsensusAccept

	require.Equal(t, channel.ConsensusObject, msg.GetObject())
	require.Equal(t, channel.ConsensusActionAccept, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Accept_Verify(t *testing.T) {
	var consensusAccept ConsensusAccept

	object, action := "consensus", "accept"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			obj, act, err := channel.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusAccept)
			require.NoError(t, err)

			err = consensusAccept.Verify()
			require.Error(t, err)
		}
	}

	t.Run("accepted try is negative", getTestBadExample("wrong_accept_negative_accepted_try.json"))
	t.Run("created at is negative", getTestBadExample("wrong_accept_negative_created_at.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_accept_not_base_64_instance.json"))
	t.Run("message is not base64", getTestBadExample("wrong_accept_not_base_64_message.json"))
}
