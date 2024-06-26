package mroot

import (
	"encoding/base64"
	"fmt"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
)

// LaoCreate defines a message data
type LaoCreate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	Organizer string   `json:"organizer"`
	Witnesses []string `json:"witnesses"`
}

// Verify verifies that the LaoCreate message is valid
func (message LaoCreate) Verify() error {
	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("lao id is %s, should be base64URL encoded", message.ID)
	}

	// verify lao id
	expectedLaoID := channel.Hash(
		message.Organizer,
		fmt.Sprintf("%d", message.Creation),
		message.Name,
	)
	if message.ID != expectedLaoID {
		return errors.NewInvalidMessageFieldError("lao id is %s, should be %s", message.ID, expectedLaoID)
	}

	// verify lao name non-empty
	if len(message.Name) == 0 {
		return errors.NewInvalidMessageFieldError("lao name is %s, should not be empty", message.Name)
	}

	// verify creation is positive
	if message.Creation < 0 {
		return errors.NewInvalidMessageFieldError("lao creation is %d, should be minimum 0", message.Creation)
	}

	// verify organizer is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.Organizer)
	if err != nil {
		return errors.NewInvalidMessageFieldError("lao organizer is %s, should be base64URL encoded", message.Organizer)
	}

	// verify all witnesses are base64URL encoded
	for _, witness := range message.Witnesses {
		_, err = base64.URLEncoding.DecodeString(witness)
		if err != nil {
			return errors.NewInvalidMessageFieldError("lao witness is %s, should be base64URL encoded", witness)
		}
	}

	return nil
}

// GetObject implements MessageData
func (LaoCreate) GetObject() string {
	return channel.LAOObject
}

// GetAction implements MessageData
func (LaoCreate) GetAction() string {
	return channel.LAOActionCreate
}

// NewEmpty implements MessageData
func (LaoCreate) NewEmpty() channel.MessageData {
	return &LaoCreate{}
}
