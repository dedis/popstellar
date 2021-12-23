package messagedata

// ElectionOpen defines a message data
type ElectionOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	LAO      string `json:"lao"`
	Election string `json:"election"`

	// OpenedAt is a Unix  timestamp
	OpenedAt int64 `json:"opened_at"`
}
