package mlao

import (
	"popstellar/internal/handler/channel"
)

// LaoGreet defines a message data
type LaoGreet struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	LaoID    string `json:"lao"`
	Frontend string `json:"frontend"`
	Address  string `json:"address"`
	Peers    []Peer `json:"peers"`
}

// Peer defines a peer server for the LAO
type Peer struct {
	Address string `json:"address"`
}

// GetObject implements MessageData
func (LaoGreet) GetObject() string {
	return channel.LAOObject
}

// GetAction implements MessageData
func (LaoGreet) GetAction() string {
	return channel.LAOActionGreet
}

// NewEmpty implements MessageData
func (LaoGreet) NewEmpty() channel.MessageData {
	return &LaoGreet{}
}
