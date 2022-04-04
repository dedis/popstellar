package messagedata

// RollCallClose defines a message data
type RollCallClose struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Closes   string `json:"closes"`

	// ClosedAt is a Unix timestamp
	ClosedAt int64 `json:"closed_at"`

	// Attendees is a list of public keys
	Attendees []string `json:"attendees"`
}

// GetObject implements MessageData
func (RollCallClose) GetObject() string {
	return RollCallObject
}

// GetAction implements MessageData
func (RollCallClose) GetAction() string {
	return RollCallActionClose
}

// NewEmpty implements MessageData
func (RollCallClose) NewEmpty() MessageData {
	return &RollCallClose{}
}
