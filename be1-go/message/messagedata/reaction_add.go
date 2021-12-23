package messagedata

// ReactionAdd defines a message data
type ReactionAdd struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	ReactionId string `json:"reaction_id"`
	ChirpId    string `json:"chirp_id"`
	Timestamp  int64  `json:"timestamp"`
}
