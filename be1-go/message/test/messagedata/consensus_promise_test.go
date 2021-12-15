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

func Test_Consensus_Promise_Not_Base64_Instance(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "wrong_promise_not_base_64_instance.json")

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
	require.Equal(t, "@@@", msg.InstanceID)
	require.Equal(t, "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=", msg.MessageID)
	require.Equal(t, int64(1634760060), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)
	require.Equal(t, int64(4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.Error(t, err, "instance id is %s, should be base64URL encoded", msg.InstanceID)
}

func Test_Consensus_Promise_Not_Base64_Message(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "wrong_promise_not_base_64_message.json")

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
	require.Equal(t, "@@@", msg.MessageID)
	require.Equal(t, int64(1634760060), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)
	require.Equal(t, int64(4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.Error(t, err, "message id is %s, should be base64URL encoded", msg.MessageID)
}

func Test_Consensus_Promise_Negative_Created_At(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "wrong_promise_negative_created_at.json")

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
	require.Equal(t, int64(-1634760060), msg.CreatedAt)
	require.Equal(t, int64(4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)
	require.Equal(t, int64(4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.Error(t, err, "created at is %d, should be minimum 0", msg.CreatedAt)
}

func Test_Consensus_Promise_Too_Low_Accepted_Try(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "wrong_promise_negative_accepted_try.json")

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
	require.Equal(t, int64(-4), msg.Value.AcceptedTry)
	require.True(t, msg.Value.AcceptedValue)
	require.Equal(t, int64(4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.Error(t, err, "accepted try is %d, should be minimum -1", msg.Value.AcceptedTry)
}

func Test_Consensus_Promise_Negative_Promised_Try(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "consensus_promise", "wrong_promise_negative_promised_try.json")

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
	require.Equal(t, int64(-4), msg.Value.PromisedTry)

	err = msg.Verify()
	require.Error(t, err, "promised try is %d, should be minimum 1", msg.Value.AcceptedTry)
}
