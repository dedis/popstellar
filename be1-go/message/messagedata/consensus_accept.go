package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusAccept defines a message data
type ConsensusAccept struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValueAccept `json:"value"`
}

type ValueAccept struct {
	AcceptedTry   int64 `json:"accepted_try"`
	AcceptedValue bool  `json:"accepted_value"`
}

// Verify verifies that the ConsensusAccept message is correct
func (message ConsensusAccept) Verify() error {
	// verify that the instance id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.InstanceID); err != nil {
		return xerrors.Errorf("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.MessageID); err != nil {
		return xerrors.Errorf("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that created at is positive
	if message.CreatedAt < 0 {
		return xerrors.Errorf("created at is %d, should be minimum 0", message.CreatedAt)
	}

	// verify that accepted try is greater or equal than -1
	if message.Value.AcceptedTry < -1 {
		return xerrors.Errorf("accepted try is %d, should be minimum -1", message.Value.AcceptedTry)
	}
	return nil
}
