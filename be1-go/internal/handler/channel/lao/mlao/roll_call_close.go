package mlao

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
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

func (message RollCallClose) Verify(laoPath string) error {
	_, err := base64.URLEncoding.DecodeString(message.UpdateID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
	}

	expectedID := channel.Hash(
		RollCallFlag,
		strings.ReplaceAll(laoPath, channel.RootPrefix, ""),
		message.Closes,
		strconv.Itoa(int(message.ClosedAt)),
	)
	if message.UpdateID != expectedID {
		return errors.NewInvalidMessageFieldError("roll call update id is %s, should be %s", message.UpdateID, expectedID)
	}

	_, err = base64.URLEncoding.DecodeString(message.Closes)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode roll call closes: %v", err)
	}

	if message.ClosedAt < 0 {
		return errors.NewInvalidMessageFieldError("roll call closed at is %d, should be minimum 0", message.ClosedAt)
	}

	return nil
}

// GetObject implements MessageData
func (RollCallClose) GetObject() string {
	return channel.RollCallObject
}

// GetAction implements MessageData
func (RollCallClose) GetAction() string {
	return channel.RollCallActionClose
}

// NewEmpty implements MessageData
func (RollCallClose) NewEmpty() channel.MessageData {
	return &RollCallClose{}
}
