package messagedata

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
