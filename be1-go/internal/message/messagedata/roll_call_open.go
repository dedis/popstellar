package messagedata

import (
	"encoding/base64"
	"popstellar/internal/message/answer"
	"strconv"
	"strings"
)

// RollCallOpen defines a message data
type RollCallOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Opens    string `json:"opens"`

	// OpenedAt is a Unix timestamp
	OpenedAt int64 `json:"opened_at"`
}

func (message RollCallOpen) Verify(laoPath string) *answer.Error {
	var errAnswer *answer.Error
	_, err := base64.URLEncoding.DecodeString(message.UpdateID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
		return errAnswer
	}
	expectedID := Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, RootPrefix, ""),
		message.Opens,
		strconv.Itoa(int(message.OpenedAt)),
	)
	if message.UpdateID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call update id is %s, should be %s", message.UpdateID, expectedID)
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(message.Opens)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call opens: %v", err)
		return errAnswer
	}

	if message.OpenedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call opened at is %d, should be minimum 0", message.OpenedAt)
		return errAnswer
	}
	return nil
}

// GetObject implements MessageData
func (RollCallOpen) GetObject() string {
	return RollCallObject
}

// GetAction implements MessageData
func (RollCallOpen) GetAction() string {
	return RollCallActionOpen
}

// NewEmpty implements MessageData
func (RollCallOpen) NewEmpty() MessageData {
	return &RollCallOpen{}
}
