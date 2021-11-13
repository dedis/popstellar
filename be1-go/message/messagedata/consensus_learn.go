package messagedata

import (
	"encoding/base64"

	"golang.org/x/xerrors"
)

// ConsensusLearn defines a message data
type ConsensusLearn struct {
	Object     string   `json:"object"`
	Action     string   `json:"action"`
	InstanceID string   `json:"instance_id"`
	MessageID  string   `json:"message_id"`
	Acceptors  []string `json:"acceptors"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValueLearn `json:"value"`

	AcceptorSignatures []string `json:"acceptor_signatures"`
}

type ValueLearn struct {
	Decision bool `json:"decision"`
}

// Verify verifies that the ConsensusLearn message is correct
func (message ConsensusLearn) Verify() error {
	// verify that the instance id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.InstanceID); err != nil {
		return xerrors.Errorf("instance id is %s, should be base64URL encoded", message.InstanceID)
	}

	// verify that the message id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.MessageID); err != nil {
		return xerrors.Errorf("message id is %s, should be base64URL encoded", message.MessageID)
	}

	// verify that the acceptors are base64URL encoded
	for acceptor := range message.Acceptors {
		if _, err := base64.URLEncoding.DecodeString(message.Acceptors[acceptor]); err != nil {
			return xerrors.Errorf("acceptor id is %s, should be base64URL encoded", message.Acceptors[acceptor])
		}
	}

	return nil
}
