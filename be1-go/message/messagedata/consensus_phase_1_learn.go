package messagedata

// ConsensusPhase1Learn defines a message data
type ConsensusPhase1Learn struct {
	Object    string   `json:"object"`
	Action    string   `json:"action"`
	MessageID string   `json:"message_id"`
	Acceptors []string `json:"acceptors"`
}
