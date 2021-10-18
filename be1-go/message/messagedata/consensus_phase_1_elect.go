package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
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

	expectedStringID := message.Object
	expectedStringID = expectedStringID + fmt.Sprintf("%d", message.CreatedAt)
	expectedStringID = expectedStringID + message.Key.Type
	expectedStringID = expectedStringID + message.Key.ID
	expectedStringID = expectedStringID + message.Key.Property
	expectedStringID = expectedStringID + message.Value
	h := sha256.New()
	h.Write([]byte(expectedStringID))

	expectedID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.InstanceID != expectedID {
		return xerrors.Errorf("invalid ConsensusStart message: invalid ID")
	}

	return nil
}
