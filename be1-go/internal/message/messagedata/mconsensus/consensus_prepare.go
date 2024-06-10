package mconsensus

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
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

// ValuePrepare defines the id of the proposition
type ValuePrepare struct {
	ProposedTry int64 `json:"proposed_try"`
}

// Verify implements Verifiable. It verifies that the ConsensusPrepare message
// is correct
func (message ConsensusPrepare) Verify() error {
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

	// verify that the proposed try is greater or equal than 1
	if message.Value.ProposedTry < 1 {
		return errors.NewInvalidMessageFieldError("proposed try is %d, should be minimum 1", message.Value.ProposedTry)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusPrepare) GetObject() string {
	return messagedata.ConsensusObject
}

// GetAction implements MessageData
func (ConsensusPrepare) GetAction() string {
	return messagedata.ConsensusActionPrepare
}

// NewEmpty implements MessageData
func (ConsensusPrepare) NewEmpty() messagedata.MessageData {
	return &ConsensusPrepare{}
}
