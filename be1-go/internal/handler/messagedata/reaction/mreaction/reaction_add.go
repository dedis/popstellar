package mreaction

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
)

// ReactionAdd defines a message data
type ReactionAdd struct {
	Object            string `json:"object"`
	Action            string `json:"action"`
	ReactionCodepoint string `json:"reaction_codepoint"`
	ChirpID           string `json:"chirp_id"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify verifies that the ReactionAdd message is correct
func (message ReactionAdd) Verify() error {
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
func (ReactionAdd) GetObject() string {
	return messagedata.ReactionObject
}

// GetAction implements MessageData
func (ReactionAdd) GetAction() string {
	return messagedata.ReactionActionAdd
}

// NewEmpty implements MessageData
func (ReactionAdd) NewEmpty() messagedata.MessageData {
	return &ReactionAdd{}
}
