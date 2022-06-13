package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Consensus_Promise(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "promise.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "consensus", object)
	require.Equal(t, "promise", action)

	var msg messagedata.ConsensusPromise

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "consensus", msg.Object)
	require.Equal(t, "promise", msg.Action)
	require.Equal(t, "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs=", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760060), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)
	require.Equal(t, int64(4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.NoError(t, err)
}

func Test_Consensus_Promise_Interface_Functions(t *testing.T) {
	var msg messagedata.ConsensusPromise

	require.Equal(t, messagedata.ConsensusObject, msg.GetObject())
	require.Equal(t, messagedata.ConsensusActionPromise, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Consensus_Promise_Verify(t *testing.T) {
	var consensusPromise messagedata.ConsensusPromise

	object, action := "consensus", "promise"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "consensus_promise", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &consensusPromise)
			require.NoError(t, err)

			err = consensusPromise.Verify()
			require.Error(t, err)
		}
	}

	t.Run("accepted try is negative", getTestBadExample("wrong_promise_negative_accepted_try.json"))
	t.Run("created at is negative", getTestBadExample("wrong_promise_negative_created_at.json"))
	t.Run("promised try is negative", getTestBadExample("wrong_promise_negative_promised_try.json"))
	t.Run("instance is not base64", getTestBadExample("wrong_promise_not_base_64_instance.json"))
	t.Run("message is not base64", getTestBadExample("wrong_promise_not_base_64_message.json"))
}
