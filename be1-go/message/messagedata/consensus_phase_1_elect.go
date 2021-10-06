package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"golang.org/x/xerrors"
)

// ConsensusStart defines a message data
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

	h := sha256.New()
	h.Write([]byte(message.Object))
	h.Write([]byte(fmt.Sprintf("%d", message.CreatedAt)))
	h.Write([]byte(message.Key.Type))
	h.Write([]byte(message.Key.ID))
	h.Write([]byte(message.Key.Property))
	h.Write([]byte(message.Value))

	testConsensusId := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.InstanceID != testConsensusId {
		return xerrors.Errorf("invalid ConsensusStart message: invalid ID")
	}

	return nil
}
