package messagedata

import (
	"encoding/json"
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
