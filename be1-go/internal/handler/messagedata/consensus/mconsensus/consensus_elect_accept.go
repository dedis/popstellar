package mconsensus

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
)

// ConsensusElectAccept defines a message data
type ConsensusElectAccept struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`
	Accept     bool   `json:"accept"`
}

// Verify implements Verifiable. It verifies that the ConsensusElectAccept
// message is correct
func (message ConsensusElectAccept) Verify() error {
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

	return nil
}

// GetObject implements MessageData
func (ConsensusElectAccept) GetObject() string {
	return messagedata.ConsensusObject
}

// GetAction implements MessageData
func (ConsensusElectAccept) GetAction() string {
	return messagedata.ConsensusActionElectAccept
}

// NewEmpty implements MessageData
func (ConsensusElectAccept) NewEmpty() messagedata.MessageData {
	return &ConsensusElectAccept{}
}
