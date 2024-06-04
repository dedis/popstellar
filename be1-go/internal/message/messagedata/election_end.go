package messagedata

import (
	"encoding/base64"
	"popstellar/internal/message/answer"
	"strings"
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
	return ElectionObject
}

// GetAction implements MessageData
func (ElectionEnd) GetAction() string {
	return ElectionActionEnd
}

// NewEmpty implements MessageData
func (ElectionEnd) NewEmpty() MessageData {
	return &ElectionEnd{}
}

func (message ElectionEnd) Verify(electionPath string) *answer.Error {
	var errAnswer *answer.Error

	_, err := base64.URLEncoding.DecodeString(message.Lao)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode lao: %v", err)
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(message.Election)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode election: %v", err)
		return errAnswer
	}

	noRoot := strings.ReplaceAll(electionPath, RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		errAnswer = answer.NewInvalidMessageFieldError("failed to split channel: %v", message)
		return errAnswer
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if message.Lao != laoID {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is not the same as the channel")
		return errAnswer
	}

	// verify if election id is the same as the channel
	if message.Election != electionID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is not the same as the channel")
		return errAnswer
	}

	// verify message created at is positive
	if message.CreatedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("message created at is negative")
		return errAnswer
	}

	// verify registered votes are base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(message.RegisteredVotes); err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("registered votes are not base64 encoded")
		return errAnswer
	}

	return nil
}
