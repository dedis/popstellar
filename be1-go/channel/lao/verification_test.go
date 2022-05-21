package lao

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"

	"github.com/stretchr/testify/require"
)

var relativeExamplePath string

func init() {
	relativeExamplePath = filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData")
}

func TestVerify_LaoState(t *testing.T) {
	// create the channel
	laoChannel, ok := newFakeChannel(t).(*Channel)
	require.True(t, ok)

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "lao_state", "lao_state.json"))
	require.NoError(t, err)

	// object and action
	object, action := "lao", "state"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var laoState messagedata.LaoState

	err = json.Unmarshal(buf, &laoState)
	require.NoError(t, err)

	// test valid example
	err = laoChannel.verifyMessageLaoState(laoState)
	require.NoError(t, err)

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err = os.ReadFile(filepath.Join(relativeExamplePath, "lao_state", file))
			require.NoError(t, err)

			obj, act, err = messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &laoState)
			require.NoError(t, err)

			err = laoChannel.verifyMessageLaoState(laoState)
			require.Error(t, err)
		}
	}

	t.Run("id not base64", getTestBadExample("bad_lao_state_id_not_base64.json"))
	t.Run("id invalid hash", getTestBadExample("bad_lao_state_id_invalid_hash.json"))
	t.Run("empty lao name", getTestBadExample("bad_lao_state_empty_name.json"))
	t.Run("creation negative", getTestBadExample("bad_lao_state_last_modified_negative.json"))
	t.Run("last modified negative", getTestBadExample("bad_lao_state_creation_negative.json"))
	t.Run("creation after last modified", getTestBadExample("bad_lao_state_creation_after_last_modified.json"))
	t.Run("organizer id not base64", getTestBadExample("bad_lao_state_organizer_not_base64.json"))
	t.Run("witness id not base64", getTestBadExample("bad_lao_state_witness_not_base64.json"))
}

func TestVerify_RollCallCreate(t *testing.T) {
	// create the channel
	laoChannel, ok := newFakeChannel(t).(*Channel)
	require.True(t, ok)

	// read the valid example file
	file := filepath.Join(relativeExamplePath, "roll_call_create.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// object and action
	object, action := "roll_call", "create"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var rollCallCreate messagedata.RollCallCreate

	err = json.Unmarshal(buf, &rollCallCreate)
	require.NoError(t, err)

	// test valid example
	err = laoChannel.verifyMessageRollCallCreate(&rollCallCreate)
	require.NoError(t, err)
}

func TestVerify_RollCallOpen(t *testing.T) {
	// create the channel
	laoChannel, ok := newFakeChannel(t).(*Channel)
	require.True(t, ok)

	// read the valid example file
	file := filepath.Join(relativeExamplePath, "roll_call_open.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// object and action
	object, action := "roll_call", "open"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var rollCallOpen messagedata.RollCallOpen

	err = json.Unmarshal(buf, &rollCallOpen)
	require.NoError(t, err)

	// test valid example
	err = laoChannel.verifyMessageRollCallOpen(rollCallOpen)
	require.NoError(t, err)
}

func TestVerify_RollCallClose(t *testing.T) {
	// create the channel
	laoChannel, ok := newFakeChannel(t).(*Channel)
	require.True(t, ok)

	// read the valid example file
	file := filepath.Join(relativeExamplePath, "roll_call_close.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// object and action
	object, action := "roll_call", "close"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var rollCallClose messagedata.RollCallClose

	err = json.Unmarshal(buf, &rollCallClose)
	require.NoError(t, err)

	// test valid example
	err = laoChannel.verifyMessageRollCallClose(&rollCallClose)
	require.NoError(t, err)
}

func TestVerify_ElectionSetup(t *testing.T) {
	// create the channel
	laoChannel, ok := newFakeChannel(t).(*Channel)
	require.True(t, ok)

	// read the valid example file
	buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "election_setup", "election_setup.json"))
	require.NoError(t, err)

	// object and action
	object, action := "election", "setup"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionSetup messagedata.ElectionSetup

	err = json.Unmarshal(buf, &electionSetup)
	require.NoError(t, err)

	// test valid example
	err = laoChannel.verifyMessageElectionSetup(electionSetup)
	require.NoError(t, err)

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err = os.ReadFile(filepath.Join(relativeExamplePath, "election_setup", file))
			require.NoError(t, err)

			obj, act, err = messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &electionSetup)
			require.NoError(t, err)

			err = laoChannel.verifyMessageElectionSetup(electionSetup)
			require.Error(t, err)
		}
	}

	t.Run("lao id not base64", getTestBadExample("bad_election_setup_lao_id_not_base64.json"))
	t.Run("lao id invalid hash", getTestBadExample("bad_election_setup_lao_id_invalid_hash.json"))
	t.Run("election id not base64", getTestBadExample("bad_election_setup_id_not_base64.json"))
	t.Run("election id invalid hash", getTestBadExample("bad_election_setup_id_invalid_hash.json"))
	t.Run("election name empty", getTestBadExample("bad_election_setup_name_empty.json"))
	t.Run("created at negative", getTestBadExample("bad_election_setup_created_at_negative.json"))
	t.Run("start time negative", getTestBadExample("bad_election_setup_start_time_negative.json"))
	t.Run("end time negative", getTestBadExample("bad_election_setup_end_time_negative.json"))
	t.Run("start time before created at", getTestBadExample("bad_election_setup_start_time_before_created_at.json"))
	t.Run("end time before created at", getTestBadExample("bad_election_setup_end_time_before_created_at.json"))
	t.Run("question id not base64", getTestBadExample("bad_election_setup_question_id_not_base64.json"))
	t.Run("question id invalid hash", getTestBadExample("bad_election_setup_question_id_invalid_hash.json"))
	t.Run("question empty", getTestBadExample("bad_election_setup_question_empty.json"))
	t.Run("voting method invalid", getTestBadExample("bad_election_setup_question_voting_method_invalid.json"))
}

// -----------------------------------------------------------------------------
// Utility functions

func newFakeChannel(t *testing.T) channel.Channel {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	numMessages := 1

	messages := make([]message.Message, numMessages)

	channel, err := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, messages[0], nolog, keypair.public, nil)
	require.NoError(t, err)

	return channel
}
