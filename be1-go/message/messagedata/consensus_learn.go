package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusLearn defines a message data
type ConsensusLearn struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValueLearn `json:"value"`

	AcceptorSignatures []string `json:"acceptor-signatures"`
}

type ValueLearn struct {
	Decision bool `json:"decision"`
}

// Verify verifies that the ConsensusLearn message is correct
// Verify implements Verifiable
func (message ConsensusLearn) Verify() error {
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

	// verify that the acceptors are base64URL encoded
	for acceptor := range message.AcceptorSignatures {
		_, err := base64.URLEncoding.DecodeString(message.AcceptorSignatures[acceptor])
		if err != nil {
			return xerrors.Errorf("acceptor id is %s, should be base64URL encoded", message.AcceptorSignatures[acceptor])
		}
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusLearn) GetObject() string {
	return ConsensusObject
}

// GetAction implements MessageData
func (ConsensusLearn) GetAction() string {
	return ConsensusActionLearn
}

// NewEmpty implements MessageData
func (ConsensusLearn) NewEmpty() MessageData {
	return &ConsensusLearn{}
}
