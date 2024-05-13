package messagedata

// FederationChallenge defines a message data
type FederationChallenge struct {
	Object string `json:"object"`
	Action string `json:"action"`

	// Value is a 32 bytes array encoded in hexadecimal
	Value      string `json:"value"`
	ValidUntil int64  `json:"valid_until"`
}

// GetObject implements MessageData
func (FederationChallenge) GetObject() string {
	return FederationObject
}

// GetAction implements MessageData
func (FederationChallenge) GetAction() string {
	return FederationActionChallenge
}

// NewEmpty implements MessageData
func (FederationChallenge) NewEmpty() MessageData {
	return &FederationChallenge{}
}
