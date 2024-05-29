package messagedata

import (
	"encoding/hex"
	"popstellar/message/answer"
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
	return FederationObject
}

// GetAction implements MessageData
func (FederationChallenge) GetAction() string {
	return FederationActionChallenge
}

// NewEmpty implements MessageData
func (FederationChallenge) NewEmpty() MessageData {
	return &FederationChallenge{}
}

func (message FederationChallenge) Verify() *answer.Error {
	if message.Object != message.GetObject() {
		return answer.NewInvalidMessageFieldError(
			"object is %s instead of %s",
			message.Object, message.GetAction())
	}

	if message.Action != message.GetAction() {
		return answer.NewInvalidMessageFieldError(
			"action is %s instead of %s",
			message.Action, message.GetAction())
	}

	if message.ValidUntil < 0 {
		return answer.NewInvalidMessageFieldError("valid_until is negative")
	}

	valueBytes, err := hex.DecodeString(message.Value)
	if err != nil || len(valueBytes) != 32 {
		return answer.NewInvalidMessageFieldError(
			"value is not a 32 bytes array encoded in hexadecimal")
	}

	return nil
}
