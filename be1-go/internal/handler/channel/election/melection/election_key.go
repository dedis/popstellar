package melection

import (
	"popstellar/internal/handler/channel"
)

// ElectionKey defines a message data
type ElectionKey struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Election string `json:"election"`
	Key      string `json:"election_key"`
}

// GetObject implements MessageData
func (ElectionKey) GetObject() string {
	return messagedata.ElectionObject
}

// GetAction implements MessageData
func (ElectionKey) GetAction() string {
	return messagedata.ElectionActionKey
}

// NewEmpty implements MessageData
func (ElectionKey) NewEmpty() messagedata.MessageData {
	return &ElectionKey{}
}
