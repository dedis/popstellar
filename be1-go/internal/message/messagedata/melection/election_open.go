package melection

import (
	"encoding/base64"
	"popstellar/internal/message/messagedata"
	"strings"

	"popstellar/internal/errors"
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
	return messagedata.ElectionObject
}

// GetAction implements MessageData
func (ElectionOpen) GetAction() string {
	return messagedata.ElectionActionOpen
}

// NewEmpty implements MessageData
func (ElectionOpen) NewEmpty() messagedata.MessageData {
	return &ElectionOpen{}
}

func (message ElectionOpen) Verify(electionPath string) error {
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
		return errors.NewInvalidMessageFieldError("failed to split channel: %v", electionPath)
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

	// verify opened at is positive
	if message.OpenedAt < 0 {
		return errors.NewInvalidMessageFieldError("opened at is negative")
	}

	return nil
}
