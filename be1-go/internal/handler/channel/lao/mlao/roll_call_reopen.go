package mlao

import (
	"popstellar/internal/handler/channel"
)

// RollCallReOpen defines a message data
type RollCallReOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Opens    string `json:"opens"`

	// OpenedAt is a Unix timestamp
	OpenedAt int64 `json:"opened_at"`
}

// GetObject implements MessageData
func (RollCallReOpen) GetObject() string {
	return channel.RollCallObject
}

// GetAction implements MessageData
func (RollCallReOpen) GetAction() string {
	return channel.RollCallActionReOpen
}

// NewEmpty implements MessageData
func (RollCallReOpen) NewEmpty() channel.MessageData {
	return &RollCallReOpen{}
}
