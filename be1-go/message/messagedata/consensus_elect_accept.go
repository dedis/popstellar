package messagedata

// ConsensusElectAccept defines a message data
type ConsensusElectAccept struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`
	Accept    bool   `json:"accept"`
}
