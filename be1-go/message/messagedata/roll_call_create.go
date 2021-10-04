package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"golang.org/x/xerrors"
)

// RollCallCreate defines a message data
type RollCallCreate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// ProposedStart is a Unix timestamp
	ProposedStart int64 `json:"proposed_start"`

	// ProposedEnd is a Unix timestamp
	ProposedEnd int64 `json:"proposed_end"`

	Location    string `json:"location"`
	Description string `json:"description"`
}

// Verify that the RollCallCreate message is valid
func (message RollCallCreate) Verify(channelID string) error {

	h := sha256.New()
	h.Write([]byte("R"))
	h.Write([]byte(channelID))
	h.Write([]byte(fmt.Sprintf("%d", message.Creation)))
	h.Write([]byte(message.Name))
	testRollCallID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.ID != testRollCallID {
		return xerrors.Errorf("invalid RollCallCreate message: invalid ID")
	}

	return nil
}
