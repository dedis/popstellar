package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"

	"golang.org/x/xerrors"
)

const (
	ConsensusObject            = "consensus"
	ConsensusActionAccept      = "accept"
	ConsensusActionElect       = "elect"
	ConsensusActionElectAccept = "elect_accept"
	ConsensusActionFailure     = "failure"
	ConsensusActionLearn       = "learn"
	ConsensusActionPrepare     = "prepare"
	ConsensusActionPromise     = "promise"
	ConsensusActionPropose     = "propose"

	ElectionObject       = "election"
	ElectionActionSetup  = "setup"
	ElectionActionKey    = "key"
	ElectionActionOpen   = "open"
	ElectionActionEnd    = "end"
	ElectionActionResult = "result"

	LAOObject       = "lao"
	LAOActionCreate = "create"
	LAOActionState  = "state"
	LAOActionUpdate = "update_properties"
	LAOActionGreet  = "greet"

	MeetingObject       = "meeting"
	MeetingActionCreate = "create"
	MeetingActionState  = "state"

	MessageObject        = "message"
	MessageActionWitness = "witness"

	RollCallObject       = "roll_call"
	RollCallActionClose  = "close"
	RollCallActionCreate = "create"
	RollCallActionOpen   = "open"
	RollCallActionReOpen = "reopen"

	VoteActionCastVote = "cast_vote"
	VoteActionWriteIn  = "write_in"

	ChirpObject             = "chirp"
	ChirpActionAdd          = "add"
	ChirpActionDelete       = "delete"
	ChirpActionNotifyAdd    = "notify_add"
	ChirpActionNotifyDelete = "notify_delete"

	ReactionObject       = "reaction"
	ReactionActionAdd    = "add"
	ReactionActionDelete = "delete"

	CoinObject                = "coin"
	CoinActionPostTransaction = "post_transaction"
	// RootPrefix denotes the prefix for the root channel, used to verify the
	// channel of origin of some message
	RootPrefix = "/root/"

	ServerObject      = "server"
	ServerActionGreet = "greet"
)

// Peer defines a peer server for the LAO
type Peer struct {
	Address string `json:"address"`
}

// MessageData defines a common interface for message data to be used with a
// registry.
type MessageData interface {
	GetObject() string
	GetAction() string
	NewEmpty() MessageData
}

// Verifiable defines a MessageData that offers message verification
type Verifiable interface {
	MessageData
	Verify() error
}

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

// GetTime returns the time of a JSON RPC message.
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

// Hash returns the sha256 created from an array of strings
func Hash(strs ...string) string {
	h := sha256.New()
	for _, s := range strs {
		h.Write([]byte(fmt.Sprintf("%d", len(s))))
		h.Write([]byte(s))
	}

	return base64.URLEncoding.EncodeToString(h.Sum(nil))
}
