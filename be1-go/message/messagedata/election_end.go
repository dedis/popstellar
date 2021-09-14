package messagedata

// ElectionEnd ...
type ElectionEnd struct {
	Object          string `json:"object"`
	Action          string `json:"action"`
	LAO             string `json:"lao"`
	Election        string `json:"election"`
	CreatedAt       int64  `json:"created_at"`
	RegisteredVotes string `json:"registered_votes"`
}
