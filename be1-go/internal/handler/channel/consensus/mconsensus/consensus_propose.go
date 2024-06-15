package mconsensus

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
)

// ConsensusPropose defines a message data
type ConsensusPropose struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePropose `json:"value"`

	AcceptorSignatures []string `json:"acceptor-signatures"`
}

type ValuePropose struct {
	ProposedTry   int64 `json:"proposed_try"`
	ProposedValue bool  `json:"proposed_value"`
}

// Verify implements Verifiable. It verifies that the ConsensusPropose message
// is correct
func (message ConsensusPropose) Verify() error {
	// verify that the instance id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.InstanceID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.MessageID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that created at is positive
	if message.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("created at is %d, should be minimum 0", message.CreatedAt)
	}

	// verify that the proposed try is positive
	if message.Value.ProposedTry < 0 {
		return errors.NewInvalidMessageFieldError("proposed try is %d, should be minimum 0", message.Value.ProposedTry)
	}

	// verify that the acceptors are base64URL encoded
	for acceptor := range message.AcceptorSignatures {
		_, err = base64.URLEncoding.DecodeString(message.AcceptorSignatures[acceptor])
		if err != nil {
			return errors.NewInvalidMessageFieldError("acceptor id is %s, should be base64URL encoded",
				message.AcceptorSignatures[acceptor])
		}
	}

	return nil
}

// GetObject implements MessageData
func (ConsensusPropose) GetObject() string {
	return channel.ConsensusObject
}

// GetAction implements MessageData
func (ConsensusPropose) GetAction() string {
	return channel.ConsensusActionPropose
}

// NewEmpty implements MessageData
func (ConsensusPropose) NewEmpty() channel.MessageData {
	return &ConsensusPropose{}
}
