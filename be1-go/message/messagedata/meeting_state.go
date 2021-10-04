package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"

	"golang.org/x/xerrors"
)

// MeetingState defines a message data
type MeetingState struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	ID       string `json:"id"`
	Name     string `json:"name"`
	Location string `json:"location"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// LastModified is a Unix timestamp
	LastModified int64 `json:"last_modified"`

	// Start is a Unix timestamp
	Start int64 `json:"start"`

	// End is a Unix timestamp
	End int64 `json:"end"`

	Extra                  json.RawMessage         `json:"extra"`
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}

// Verify that the MeetingState message is valid
func (message MeetingState) Verify(channelID string) error {

	h := sha256.New()
	h.Write([]byte("M"))
	h.Write([]byte(channelID))
	h.Write([]byte(fmt.Sprintf("%d", message.Creation)))
	h.Write([]byte(message.Name))
	testMeetingID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.ID != testMeetingID {
		return xerrors.Errorf("invalid MeetingState message: invalid ID")
	}

	return nil
}
