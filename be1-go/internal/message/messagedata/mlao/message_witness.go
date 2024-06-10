package mlao

import (
	"popstellar/internal/message/mmessage"
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
	return mmessage.MessageObject
}

// GetAction implements MessageData
func (MessageWitness) GetAction() string {
	return mmessage.MessageActionWitness
}

// NewEmpty implements MessageData
func (MessageWitness) NewEmpty() mmessage.MessageData {
	return &MessageWitness{}
}
