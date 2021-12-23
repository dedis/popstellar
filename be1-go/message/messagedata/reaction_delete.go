package messagedata

// ReactionDelete defines a message data
type ReactionDelete struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	ReactionId string `json:"reaction_id"`
	Timestamp  int64  `json:"timestamp"`
}
