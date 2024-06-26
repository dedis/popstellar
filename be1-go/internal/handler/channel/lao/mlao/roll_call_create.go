package mlao

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"strconv"
	"strings"
)

// RollCallCreate defines a message data
type RollCallCreate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// ProposedStart is a Unix timestamp
	ProposedStart int64 `json:"proposed_start"`

	// ProposedEnd is a Unix timestamp
	ProposedEnd int64 `json:"proposed_end"`

	Location    string `json:"location"`
	Description string `json:"description"`
}

const RollCallFlag = "R"

func (message RollCallCreate) Verify(laoPath string) error {
	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode roll call ID: %v", err)
	}

	// verify roll call create message id
	expectedID := channel.Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, channel.RootPrefix, ""),
		strconv.Itoa(int(message.Creation)),
		message.Name,
	)

	if message.ID != expectedID {
		return errors.NewInvalidMessageFieldError("roll call id is %s, should be %s", message.ID, expectedID)
	}

	// verify creation is positive
	if message.Creation < 0 {
		return errors.NewInvalidMessageFieldError("roll call creation is %d, should be minimum 0", message.Creation)
	}

	// verify proposed start after creation
	if message.ProposedStart < message.Creation {
		return errors.NewInvalidMessageFieldError("roll call proposed start time should be greater than creation time")
	}

	// verify proposed end after proposed start
	if message.ProposedEnd < message.ProposedStart {
		return errors.NewInvalidMessageFieldError("roll call proposed end should be greater than proposed start")
	}

	return nil
}

// GetObject implements MessageData
func (RollCallCreate) GetObject() string {
	return channel.RollCallObject
}

// GetAction implements MessageData
func (RollCallCreate) GetAction() string {
	return channel.RollCallActionCreate
}

// NewEmpty implements MessageData
func (RollCallCreate) NewEmpty() channel.MessageData {
	return &RollCallCreate{}
}
