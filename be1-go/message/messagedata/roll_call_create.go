package messagedata

import (
	"encoding/base64"
	"popstellar/message/answer"
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

func (message RollCallCreate) Verify(laoPath string) *answer.Error {
	var errAnswer *answer.Error
	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call ID: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify roll call create message id
	expectedID := Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, RootPrefix, ""),
		strconv.Itoa(int(message.Creation)),
		message.Name,
	)
	if message.ID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call id is %s, should be %s", message.ID, expectedID)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify creation is positive
	if message.Creation < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call creation is %d, should be minimum 0", message.Creation)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify proposed start after creation
	if message.ProposedStart < message.Creation {
		errAnswer = answer.NewInvalidMessageFieldError("roll call proposed start time should be greater than creation time")
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify proposed end after proposed start
	if message.ProposedEnd < message.ProposedStart {
		errAnswer = answer.NewInvalidMessageFieldError("roll call proposed end should be greater than proposed start")
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}
	return nil
}

// GetObject implements MessageData
func (RollCallCreate) GetObject() string {
	return RollCallObject
}

// GetAction implements MessageData
func (RollCallCreate) GetAction() string {
	return RollCallActionCreate
}

// NewEmpty implements MessageData
func (RollCallCreate) NewEmpty() MessageData {
	return &RollCallCreate{}
}
