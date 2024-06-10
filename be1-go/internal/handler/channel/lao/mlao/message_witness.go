package mlao

import (
	"popstellar/internal/handler/channel"
)

// MessageWitness defines a message data
type MessageWitness struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`
	Signature string `json:"signature"`
}

// GetObject implements MessageData
func (MessageWitness) GetObject() string {
	return messagedata.MessageObject
}

// GetAction implements MessageData
func (MessageWitness) GetAction() string {
	return messagedata.MessageActionWitness
}

// NewEmpty implements MessageData
func (MessageWitness) NewEmpty() messagedata.MessageData {
	return &MessageWitness{}
}
