package messagedata

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"golang.org/x/xerrors"
)

// LaoCreate defines a message data
type LaoCreate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	Organizer string   `json:"organizer"`
	Witnesses []string `json:"witnesses"`
}

// Verify that the LaoCreate message is valid
func (message LaoCreate) Verify() error {

	h := sha256.New()
	h.Write([]byte(message.Organizer))
	h.Write([]byte(fmt.Sprintf("%d", message.Creation)))
	h.Write([]byte(message.Name))
	testLaoID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	if message.ID != testLaoID {
		return xerrors.Errorf("ID %s do not correspond with message data", message.ID)
	}

	return nil
}
