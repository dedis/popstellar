package melection

import (
	"encoding/base64"
	"popstellar/internal/message/messagedata"
	"strings"

	"popstellar/internal/errors"
)

// ElectionEnd defines a message data
type ElectionEnd struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix  timestamp
	CreatedAt int64 `json:"created_at"`

	RegisteredVotes string `json:"registered_votes"`
}

// GetObject implements MessageData
func (ElectionEnd) GetObject() string {
	return messagedata.ElectionObject
}

// GetAction implements MessageData
func (ElectionEnd) GetAction() string {
	return messagedata.ElectionActionEnd
}

// NewEmpty implements MessageData
func (ElectionEnd) NewEmpty() messagedata.MessageData {
	return &ElectionEnd{}
}

func (message ElectionEnd) Verify(electionPath string) error {
	_, err := base64.URLEncoding.DecodeString(message.Lao)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode lao: %v", err)
	}

	_, err = base64.URLEncoding.DecodeString(message.Election)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode election: %v", err)
	}

	noRoot := strings.ReplaceAll(electionPath, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return errors.NewInvalidMessageFieldError("failed to split channel: %v", message)
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if message.Lao != laoID {
		return errors.NewInvalidMessageFieldError("lao id is not the same as the channel")
	}

	// verify if election id is the same as the channel
	if message.Election != electionID {
		return errors.NewInvalidMessageFieldError("election id is not the same as the channel")
	}

	// verify message created at is positive
	if message.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("message created at is negative")
	}

	// verify registered votes are base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.RegisteredVotes); err != nil {
		return errors.NewInvalidMessageFieldError("registered votes are not base64 encoded")
	}

	return nil
}
