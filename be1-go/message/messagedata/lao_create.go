package messagedata

import (
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
	expectedLaoID := Hash([]string{
		message.Organizer,
		fmt.Sprintf("%d", message.Creation),
		message.Name,
	})

	if message.ID != expectedLaoID {
		return xerrors.Errorf("ID %s do not correspond with message data", message.ID)
	}

	return nil
}
