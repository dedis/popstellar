package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
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
		return xerrors.Errorf("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.MessageID)
	if err != nil {
		return xerrors.Errorf("message id is %s, should be base64URL encoded", message.MessageID)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusElectAccept) GetObject() string {
	return ConsensusObject
}

// GetAction implements MessageData
func (ConsensusElectAccept) GetAction() string {
	return ConsensusActionElectAccept
}

// NewEmpty implements MessageData
func (ConsensusElectAccept) NewEmpty() MessageData {
	return &ConsensusElectAccept{}
}
