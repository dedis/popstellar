package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
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

// Verify implements Verifiable. It verifies that the ConsensusElect message is
// correct
func (message ConsensusElect) Verify() error {
	// verify that the instance id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.InstanceID)
	if err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify the instance ID
	expectedID := Hash(
		message.Object,
		message.Key.Type,
		message.Key.ID,
		message.Key.Property,
	)

	if message.InstanceID != expectedID {
		return xerrors.Errorf("instance id is %s, should be %s", message.InstanceID, expectedID)
	}

	// verify CreatedAt is positive
	if message.CreatedAt < 0 {
		return xerrors.Errorf("consensus creation is %d, should be at minimum 0", message.CreatedAt)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusElect) GetObject() string {
	return ConsensusObject
}

// GetAction implements MessageData
func (ConsensusElect) GetAction() string {
	return ConsensusActionElect
}

// NewEmpty implements MessageData
func (ConsensusElect) NewEmpty() MessageData {
	return &ConsensusElect{}
}

type Key struct {
	Type     string `json:"type"`
	ID       string `json:"id"`
	Property string `json:"property"`
}
