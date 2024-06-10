package mconsensus

import (
	"encoding/base64"
	"popstellar/internal/errors"
	message2 "popstellar/internal/handler/message/mmessage"
)

// ConsensusElect defines a message data
type ConsensusElect struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	CreatedAt  int64  `json:"created_at"`

	Key Key `json:"key"`

	Value string `json:"value"`
}

// Key defines the object that the consensus refers to
type Key struct {
	Type     string `json:"type"`
	ID       string `json:"id"`
	Property string `json:"property"`
}

// Verify implements Verifiable. It verifies that the ConsensusElect message is
// correct
func (message ConsensusElect) Verify() error {
	// verify that the instance id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.InstanceID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("lao id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify the instance ID
	expectedID := message2.Hash(
		message.Object,
		message.Key.Type,
		message.Key.ID,
		message.Key.Property,
	)

	if message.InstanceID != expectedID {
		return errors.NewInvalidMessageFieldError("instance id is %s, should be %s", message.InstanceID, expectedID)
	}

	// verify CreatedAt is positive
	if message.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("consensus creation is %d, should be at minimum 0", message.CreatedAt)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusElect) GetObject() string {
	return message2.ConsensusObject
}

// GetAction implements MessageData
func (ConsensusElect) GetAction() string {
	return message2.ConsensusActionElect
}

// NewEmpty implements MessageData
func (ConsensusElect) NewEmpty() message2.MessageData {
	return &ConsensusElect{}
}
