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
	return channel.MessageObject
}

// GetAction implements MessageData
func (MessageWitness) GetAction() string {
	return channel.MessageActionWitness
}

// NewEmpty implements MessageData
func (MessageWitness) NewEmpty() channel.MessageData {
	return &MessageWitness{}
}
