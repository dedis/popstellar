package messagedata

// ConsensusPhase1ElectAccept defines a message data
type ConsensusPhase1ElectAccept struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`
	Accept    bool   `json:"accept"`
}
