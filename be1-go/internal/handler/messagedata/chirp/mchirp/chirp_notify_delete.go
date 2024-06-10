package mchirp

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
)

// ChirpNotifyDelete defines a message data
type ChirpNotifyDelete struct {
	Object  string `json:"object"`
	Action  string `json:"action"`
	ChirpID string `json:"chirp_id"`
	Channel string `json:"channel"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify implements Verifiable. It verifies that the ChirpNotifyDelete message
// is correct
func (message ChirpNotifyDelete) Verify() error {
	// verify that Timestamp is positive
	if message.Timestamp < 0 {
		return errors.NewInvalidMessageFieldError("timestamp is %d, should be minimum 0", message.Timestamp)
	}

	// verify that the chirp id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ChirpID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("chirp id is %s, should be base64URL encoded", message.ChirpID)
	}

	return nil
}

// GetObject implements MessageData
func (ChirpNotifyDelete) GetObject() string {
	return messagedata.ChirpObject
}

// GetAction implements MessageData
func (ChirpNotifyDelete) GetAction() string {
	return messagedata.ChirpActionNotifyDelete
}

// NewEmpty implements MessageData
func (ChirpNotifyDelete) NewEmpty() messagedata.MessageData {
	return &ChirpNotifyDelete{}
}
