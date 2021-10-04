package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"

	"golang.org/x/xerrors"
)

// MeetingCreate defines a message data
type MeetingCreate struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	ID       string `json:"id"`
	Name     string `json:"name"`
	Location string `json:"location"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// Start is a Unix timestamp
	Start int64 `json:"start"`

	// End is a Unix timestamp
	End int64 `json:"end"`

	Extra json.RawMessage `json:"extra"`
}

// Verify that the MeetingCreate message is valid
func (message MeetingCreate) Verify(channelID string) error {

	h := sha256.New()
	h.Write([]byte("M"))
	h.Write([]byte(channelID))
	h.Write([]byte(fmt.Sprintf("%d", message.Creation)))
	h.Write([]byte(message.Name))
	testMeetingID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.ID != testMeetingID {
		return xerrors.Errorf("invalid MeetingCreate message: invalid ID")
	}

	return nil
}
