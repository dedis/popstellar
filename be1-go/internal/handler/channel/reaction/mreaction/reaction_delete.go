package mreaction

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
)

// ReactionDelete defines a message data
type ReactionDelete struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	ReactionID string `json:"reaction_id"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify verifies that the ReactionDelete message is correct
func (message ReactionDelete) Verify() error {
	// verify that Timestamp is positive
	if message.Timestamp < 0 {
		return errors.NewInvalidMessageFieldError("timestamp is %d, should be minimum 0", message.Timestamp)
	}

	// verify that the reaction id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ReactionID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("reaction id is %s, should be base64URL encoded", message.ReactionID)
	}

	return nil
}

// GetObject implements MessageData
func (ReactionDelete) GetObject() string {
	return channel.ReactionObject
}

// GetAction implements MessageData
func (ReactionDelete) GetAction() string {
	return channel.ReactionActionDelete
}

// NewEmpty implements MessageData
func (ReactionDelete) NewEmpty() channel.MessageData {
	return &ReactionDelete{}
}
