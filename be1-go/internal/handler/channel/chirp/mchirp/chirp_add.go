package mchirp

import (
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
)

// ChirpAdd defines a message data
type ChirpAdd struct {
	Object string `json:"object"`
	Action string `json:"action"`
	Text   string `json:"text"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify implements Verifiable. It verifies that the ChirpAdd message is
// correct
func (message ChirpAdd) Verify() error {
	// verify that Timestamp is positive
	if message.Timestamp < 0 {
		return errors.NewInvalidMessageFieldError("timestamp is %d, should be minimum 0", message.Timestamp)
	}

	return nil
}

// GetObject implements MessageData
func (ChirpAdd) GetObject() string {
	return channel.ChirpObject
}

// GetAction implements MessageData
func (ChirpAdd) GetAction() string {
	return channel.ChirpActionAdd
}

// NewEmpty implements MessageData
func (ChirpAdd) NewEmpty() channel.MessageData {
	return &ChirpAdd{}
}
