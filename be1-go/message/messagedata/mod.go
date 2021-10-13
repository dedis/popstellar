package messagedata

import (
	"encoding/json"

	"golang.org/x/xerrors"
)

const (
	ElectionObject       = "election"
	ElectionActionEnd    = "end"
	ElectionActionResult = "result"
	ElectionActionSetup  = "setup"

	LAOObject       = "lao"
	LAOActionCreate = "create"
	LAOActionState  = "state"
	LAOActionUpdate = "update_properties"

	MeetingObject       = "meeting"
	MeetingActionCreate = "create"
	MeetingActionState  = "state"

	MessageObject        = "message"
	MessageActionWitness = "witness"

	RollCallObject       = "roll_call"
	RollCallActionClose  = "close"
	RollCallActionCreate = "create"
	RollCallActionOpen   = "open"
	RollCallActionReopen = "reopen"

	VoteActionCastVote = "cast_vote"
	VoteActionWriteIn  = "write_in"

	// RootPrefix denotes the prefix for the root channel, used to verify the
	// channel of origin of some message
	RootPrefix = "/root/"
)

// GetObjectAndAction returns the object and action of a JSON RPC message.
func GetObjectAndAction(buf []byte) (string, string, error) {
	var objmap map[string]json.RawMessage

	err := json.Unmarshal(buf, &objmap)
	if err != nil {
		return "", "", xerrors.Errorf("failed to unmarshal objmap: %v", err)
	}

	var object string
	var action string

	err = json.Unmarshal(objmap["object"], &object)
	if err != nil {
		return "", "", xerrors.Errorf("failed to get object: %v", err)
	}

	err = json.Unmarshal(objmap["action"], &action)
	if err != nil {
		return "", "", xerrors.Errorf("failed to get action: %v", err)
	}

	return object, action, nil
}
