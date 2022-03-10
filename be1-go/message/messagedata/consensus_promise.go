package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusPromise defines a message data
type ConsensusPromise struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePromise `json:"value"`
}

type ValuePromise struct {
	AcceptedTry   int64 `json:"accepted_try"`
	AcceptedValue bool  `json:"accepted_value"`
	PromisedTry   int64 `json:"promised_try"`
}

// Verify verifies that the ConsensusPromise message is correct
// Verify implements Verifiable
func (message ConsensusPromise) Verify() error {
	// verify that the instance id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.InstanceID)
	if err != nil {
		return xerrors.Errorf("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.MessageID)
	if err != nil {
		return xerrors.Errorf("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that created at is positive
	if message.CreatedAt < 0 {
		return xerrors.Errorf("created at is %d, should be minimum 0", message.CreatedAt)
	}

	// verify that the accepted try is greater or equal than -1
	if message.Value.AcceptedTry < -1 {
		return xerrors.Errorf("accepted try is %d, should be minimum -1", message.Value.AcceptedTry)
	}

	// verify that the promised try is greater or equal than 1
	if message.Value.PromisedTry < 1 {
		return xerrors.Errorf("promised try is %d, should be minimum 1", message.Value.PromisedTry)
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusPromise) GetObject() string {
	return ConsensusObject
}

// GetAction implements MessageData
func (ConsensusPromise) GetAction() string {
	return ConsensusActionPromise
}

// NewEmpty implements MessageData
func (ConsensusPromise) NewEmpty() MessageData {
	return &ConsensusPromise{}
}
