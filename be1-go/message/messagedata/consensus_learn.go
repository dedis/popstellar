package messagedata

// ConsensusLearn defines a message data
type ConsensusLearn struct {
	Object     string   `json:"object"`
	Action     string   `json:"action"`
	InstanceID string   `json:"instance_id"`
	MessageID  string   `json:"message_id"`
	Acceptors  []string `json:"acceptors"`
}
