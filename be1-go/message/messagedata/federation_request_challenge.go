package messagedata

// FederationRequestChallenge defines a message data
type FederationRequestChallenge struct {
	Object string `json:"object"`
	Action string `json:"action"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// GetObject implements MessageData
func (FederationRequestChallenge) GetObject() string {
	return FederationObject
}

// GetAction implements MessageData
func (FederationRequestChallenge) GetAction() string {
	return FederationActionRequestChallenge
}

// NewEmpty implements MessageData
func (FederationRequestChallenge) NewEmpty() MessageData {
	return &FederationRequestChallenge{}
}
