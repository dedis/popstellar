package mlao

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
)

// MeetingCreate defines a message data
type MeetingCreate struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	ID       string `json:"id"`
	Name     string `json:"name"`
	Location string `json:"location"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// Start is a Unix timestamp
	Start int64 `json:"start"`

	// End is a Unix timestamp
	End int64 `json:"end"`

	Extra json.RawMessage `json:"extra"`
}

// GetObject implements MessageData
func (MeetingCreate) GetObject() string {
	return channel.MeetingObject
}

// GetAction implements MessageData
func (MeetingCreate) GetAction() string {
	return channel.MeetingActionCreate
}

// NewEmpty implements MessageData
func (MeetingCreate) NewEmpty() channel.MessageData {
	return &MeetingCreate{}
}
