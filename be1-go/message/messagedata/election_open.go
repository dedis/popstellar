package messagedata

// ElectionOpen defines a message data
type ElectionOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`
}

// GetObject implements MessageData
func (ElectionOpen) GetObject() string {
	return ElectionObject
}

// GetAction implements MessageData
func (ElectionOpen) GetAction() string {
	return ElectionActionOpen
}

// NewEmpty implements MessageData
func (ElectionOpen) NewEmpty() MessageData {
	return &ElectionOpen{}
}
