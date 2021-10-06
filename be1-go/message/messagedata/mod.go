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

	ChirpObject = "chirp"
	ChirpActionAdd = "add"
	ChirpActionDelete = "delete"
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

func GetTime(buf []byte) (int64, error) {
	var objmap map[string]json.RawMessage

	err := json.Unmarshal(buf, &objmap)
	if err != nil {
		return 0, xerrors.Errorf("failed to unmarshal objmap: %v", err)
	}

	var time int64

	err = json.Unmarshal(objmap["timestamp"], &time)
	if err != nil {
		return 0, xerrors.Errorf("failed to get time: %v", err)
	}

	return time, nil
}
