package messagedata

// ElectionEnd ...
type ElectionEnd struct {
	Object          string
	Action          string
	LAO             string
	Election        string
	CreatedAt       int    `json:"created_at"`
	RegisteredVotes string `json:"registered_votes"`
}
