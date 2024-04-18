package messagedata

// FederationInit defines a message data
type FederationInit struct {
	Object string `json:"object"`
	Action string `json:"action"`

	LaoId         string `json:"lao_id"`
	ServerAddress string `json:"server_address"`
	PublicKey     string `json:"public_key"`
	ChallengeMsg  string `json:"challenge"`
}

// GetObject implements MessageData
func (FederationInit) GetObject() string {
	return FederationObject
}

// GetAction implements MessageData
func (FederationInit) GetAction() string {
	return FederationActionInit
}

// NewEmpty implements MessageData
func (FederationInit) NewEmpty() MessageData {
	return &FederationInit{}
}
