package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"golang.org/x/xerrors"
)

// RollCallOpen defines a message data
type RollCallOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Opens    string `json:"opens"`

	// OpenedAt is a Unix timestamp
	OpenedAt int64 `json:"opened_at"`
}

// Verify that the MeetingCreate message is valid
func (message RollCallOpen) Verify(channelID string) error {

	h := sha256.New()
	h.Write([]byte("R"))
	h.Write([]byte(channelID))
	h.Write([]byte(message.Opens))
	h.Write([]byte(fmt.Sprintf("%d", message.OpenedAt)))
	testUpdateID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.UpdateID != testUpdateID {
		return xerrors.Errorf("invalid RollCallOpen message: invalid ID")
	}

	return nil
}
