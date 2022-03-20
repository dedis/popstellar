package messagedata

// ElectionEnd defines a message data
type ElectionEnd struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix  timestamp
	CreatedAt int64 `json:"created_at"`

	RegisteredVotes string `json:"registered_votes"`
}

// GetObject implements MessageData
func (ElectionEnd) GetObject() string {
	return ElectionObject
}

// GetAction implements MessageData
func (ElectionEnd) GetAction() string {
	return ElectionActionEnd
}

// NewEmpty implements MessageData
func (ElectionEnd) NewEmpty() MessageData {
	return &ElectionEnd{}
}
