package messagedata

// MessageWitness defines a message data
type MessageWitness struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`
	Signature string `json:"signature"`
}

// GetObject implements MessageData
func (MessageWitness) GetObject() string {
	return MessageObject
}

// GetAction implements MessageData
func (MessageWitness) GetAction() string {
	return MessageActionWitness
}

// NewEmpty implements MessageData
func (MessageWitness) NewEmpty() MessageData {
	return &MessageWitness{}
}
