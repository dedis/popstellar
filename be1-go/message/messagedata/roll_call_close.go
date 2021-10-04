package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"golang.org/x/xerrors"
)

// RollCallClose defines a message data
type RollCallClose struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Closes   string `json:"closes"`

	// ClosedAt is a Unix timestamp
	ClosedAt int64 `json:"closed_at"`

	Attendees []string `json:"attendees"`
}

// Verify that the RollCallClose message is valid
func (message RollCallClose) Verify(channelID string) error {

	h := sha256.New()
	h.Write([]byte("R"))
	h.Write([]byte(channelID))
	h.Write([]byte(message.Closes))
	h.Write([]byte(fmt.Sprintf("%d", message.ClosedAt)))
	testUpdateID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.UpdateID != testUpdateID {
		return xerrors.Errorf("invalid RollCallCreate message: invalid ID")
	}

	return nil
}
