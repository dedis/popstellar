package messagedata

import (
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

type Key struct {
	Type     string `json:"type"`
	ID       string `json:"id"`
	Property string `json:"property"`
}

// Verify verifies that the ConsensusElect message is correct
func (message ConsensusElect) Verify() error {
	expectedID := Hash(
		message.Object,
		message.Key.Type,
		message.Key.ID,
		message.Key.Property,
	)

	if message.InstanceID != expectedID {
		return xerrors.Errorf("invalid ConsensusStart message: invalid ID")
	}

	return nil
}
