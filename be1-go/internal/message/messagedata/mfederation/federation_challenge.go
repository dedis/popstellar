package mfederation

import (
	"encoding/hex"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
)

// FederationChallenge defines a message data
type FederationChallenge struct {
	Object string `json:"object"`
	Action string `json:"action"`

	// Value is a 32 bytes array encoded in hexadecimal
	Value      string `json:"value"`
	ValidUntil int64  `json:"valid_until"`
}

// GetObject implements MessageData
func (FederationChallenge) GetObject() string {
	return mmessage.FederationObject
}

// GetAction implements MessageData
func (FederationChallenge) GetAction() string {
	return mmessage.FederationActionChallenge
}

// NewEmpty implements MessageData
func (FederationChallenge) NewEmpty() mmessage.MessageData {
	return &FederationChallenge{}
}

func (message FederationChallenge) Verify() error {
	if message.Object != message.GetObject() {
		return errors.NewInvalidMessageFieldError(
			"object is %s instead of %s",
			message.Object, message.GetAction())
	}

	if message.Action != message.GetAction() {
		return errors.NewInvalidMessageFieldError(
			"action is %s instead of %s",
			message.Action, message.GetAction())
	}

	if message.ValidUntil < 0 {
		return errors.NewInvalidMessageFieldError("valid_until is negative")
	}

	valueBytes, err := hex.DecodeString(message.Value)
	if err != nil || len(valueBytes) != 32 {
		return errors.NewInvalidMessageFieldError(
			"value is not a 32 bytes array encoded in hexadecimal")
	}

	return nil
}
