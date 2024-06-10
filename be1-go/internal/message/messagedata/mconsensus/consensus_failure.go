package mconsensus

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
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
		return errors.NewInvalidMessageFieldError("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.MessageID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that created at is positive
	if message.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("created at is %d, should be minimum 0", message.CreatedAt)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusFailure) GetObject() string {
	return messagedata.ConsensusObject
}

// GetAction implements MessageData
func (ConsensusFailure) GetAction() string {
	return messagedata.ConsensusActionFailure
}

// NewEmpty implements MessageData
func (ConsensusFailure) NewEmpty() messagedata.MessageData {
	return &ConsensusFailure{}
}
