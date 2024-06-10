package mlao

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
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

func (message RollCallOpen) Verify(laoPath string) error {
	_, err := base64.URLEncoding.DecodeString(message.UpdateID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
	}
	expectedID := messagedata.Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, messagedata.RootPrefix, ""),
		message.Opens,
		strconv.Itoa(int(message.OpenedAt)),
	)

	if message.UpdateID != expectedID {
		return errors.NewInvalidMessageFieldError("roll call update id is %s, should be %s", message.UpdateID, expectedID)
	}

	_, err = base64.URLEncoding.DecodeString(message.Opens)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode roll call opens: %v", err)
	}

	if message.OpenedAt < 0 {
		return errors.NewInvalidMessageFieldError("roll call opened at is %d, should be minimum 0", message.OpenedAt)
	}

	return nil
}

// GetObject implements MessageData
func (RollCallOpen) GetObject() string {
	return messagedata.RollCallObject
}

// GetAction implements MessageData
func (RollCallOpen) GetAction() string {
	return messagedata.RollCallActionOpen
}

// NewEmpty implements MessageData
func (RollCallOpen) NewEmpty() messagedata.MessageData {
	return &RollCallOpen{}
}
