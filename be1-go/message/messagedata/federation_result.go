package messagedata

// FederationResult defines a message data
type FederationResult struct {
	Object string `json:"object"`
	Action string `json:"action"`

	Status string `json:"status"`
	Reason string `json:"reason,omitempty"`
}

// GetObject implements MessageData
func (FederationResult) GetObject() string {
	return FederationObject
}

// GetAction implements MessageData
func (FederationResult) GetAction() string {
	return FederationActionResult
}

// NewEmpty implements MessageData
func (FederationResult) NewEmpty() MessageData {
	return &FederationResult{}
}
