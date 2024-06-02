package messagedata

import (
	"encoding/base64"
	"popstellar/internal/message/answer"
	"strings"
)

// ElectionOpen defines a message data
type ElectionOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// OpenedAt is a Unix timestamp
	OpenedAt int64 `json:"opened_at"`
}

// GetObject implements MessageData
func (ElectionOpen) GetObject() string {
	return ElectionObject
}

// GetAction implements MessageData
func (ElectionOpen) GetAction() string {
	return ElectionActionOpen
}

// NewEmpty implements MessageData
func (ElectionOpen) NewEmpty() MessageData {
	return &ElectionOpen{}
}

func (message ElectionOpen) Verify(electionPath string) *answer.Error {
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
		errAnswer = answer.NewInvalidMessageFieldError("failed to split channel: %v", electionPath)
		return errAnswer
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if message.Lao != laoID {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify if election id is the same as the channel
	if message.Election != electionID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify opened at is positive
	if message.OpenedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("opened at is negative")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	return nil
}
