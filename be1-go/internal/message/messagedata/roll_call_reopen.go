package messagedata

// RollCallReOpen defines a message data
type RollCallReOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Opens    string `json:"opens"`

	// OpenedAt is a Unix timestamp
	OpenedAt int64 `json:"opened_at"`
}

// GetObject implements MessageData
func (RollCallReOpen) GetObject() string {
	return RollCallObject
}

// GetAction implements MessageData
func (RollCallReOpen) GetAction() string {
	return RollCallActionReOpen
}

// NewEmpty implements MessageData
func (RollCallReOpen) NewEmpty() MessageData {
	return &RollCallReOpen{}
}
