package messagedata

// RollCallClose defines a message data
type RollCallClose struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Closes   string `json:"closes"`

	// ClosedAt is a Unix timestamp
	ClosedAt int64 `json:"closed_at"`

	// List of the public keys
	Attendees []string `json:"attendees"`
}
