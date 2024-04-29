package messagedata

// FederationExpect defines a message data
type FederationExpect struct {
	Object string `json:"object"`
	Action string `json:"action"`

	LaoId         string    `json:"lao_id"`
	ServerAddress string    `json:"server_address"`
	PublicKey     string    `json:"public_key"`
	Challenge     Challenge `json:"challenge"`
}

// GetObject implements MessageData
func (FederationExpect) GetObject() string {
	return FederationObject
}

// GetAction implements MessageData
func (FederationExpect) GetAction() string {
	return FederationActionExpect
}

// NewEmpty implements MessageData
func (FederationExpect) NewEmpty() MessageData {
	return &FederationExpect{}
}
