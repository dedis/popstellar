package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusFailure defines a message data
type ConsensusFailure struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`
}

// Verify implements Verifiable. It verifies that the ConsensusLearn message is
// correct
func (message ConsensusFailure) Verify() error {

	// verify that the instance id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.InstanceID)
	if err != nil {
		return xerrors.Errorf("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.MessageID)
	if err != nil {
		return xerrors.Errorf("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that created at is positive
	if message.CreatedAt < 0 {
		return xerrors.Errorf("created at is %d, should be minimum 0", message.CreatedAt)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusFailure) GetObject() string {
	return ConsensusObject
}

// GetAction implements MessageData
func (ConsensusFailure) GetAction() string {
	return ConsensusActionFailure
}

// NewEmpty implements MessageData
func (ConsensusFailure) NewEmpty() MessageData {
	return &ConsensusFailure{}
}
