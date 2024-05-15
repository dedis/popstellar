package messagedata

import (
	"encoding/base64"
	"popstellar/message/answer"
	"strconv"
	"strings"
)

// RollCallClose defines a message data
type RollCallClose struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Closes   string `json:"closes"`

	// ClosedAt is a Unix timestamp
	ClosedAt int64 `json:"closed_at"`

	// Attendees is a list of public keys
	Attendees []string `json:"attendees"`
}

func (message RollCallClose) Verify(laoPath string) *answer.Error {
	var errAnswer *answer.Error
	_, err := base64.URLEncoding.DecodeString(message.UpdateID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
		return errAnswer
	}

	expectedID := Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, RootPrefix, ""),
		message.Closes,
		strconv.Itoa(int(message.ClosedAt)),
	)
	if message.UpdateID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call update id is %s, should be %s", message.UpdateID, expectedID)
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(message.Closes)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call closes: %v", err)
		return errAnswer
	}

	if message.ClosedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call closed at is %d, should be minimum 0", message.ClosedAt)
		return errAnswer
	}
	return nil
}

// GetObject implements MessageData
func (RollCallClose) GetObject() string {
	return RollCallObject
}

// GetAction implements MessageData
func (RollCallClose) GetAction() string {
	return RollCallActionClose
}

// NewEmpty implements MessageData
func (RollCallClose) NewEmpty() MessageData {
	return &RollCallClose{}
}
