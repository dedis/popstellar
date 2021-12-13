package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusPrepare defines a message data
type ConsensusPrepare struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePrepare `json:"value"`
}

type ValuePrepare struct {
	ProposedTry int64 `json:"proposed_try"`
}

// Verify that the ConsensusPrepare message is correct
func (message ConsensusPrepare) Verify() error {
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

	// verify that the proposed try is greater or equal than 1
	if message.Value.ProposedTry < 1 {
		return xerrors.Errorf("proposed try is %d, should be minimum 1", message.Value.ProposedTry)
	}

	return nil
}
