package messagedata

import (
	"fmt"

	"golang.org/x/xerrors"
)

// ConsensusPhase1Elect defines a message data
type ConsensusPhase1Elect struct {
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

func (message ConsensusPhase1Elect) Verify() error {
	expectedID := Hash([]string{
		message.Object,
		fmt.Sprintf("%d", message.CreatedAt),
		message.Key.Type,
		message.Key.ID,
		message.Key.Property,
		message.Value,
	})

	if message.InstanceID != expectedID {
		return xerrors.Errorf("invalid ConsensusStart message: invalid ID")
	}

	return nil
}
