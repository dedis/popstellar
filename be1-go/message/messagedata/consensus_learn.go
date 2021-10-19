package messagedata

// ConsensusLearn defines a message data
type ConsensusLearn struct {
	Object    string   `json:"object"`
	Action    string   `json:"action"`
	MessageID string   `json:"message_id"`
	Acceptors []string `json:"acceptors"`
}
