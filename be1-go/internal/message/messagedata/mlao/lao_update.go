package mlao

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/mmessage"
)

// LaoUpdate defines a message data
type LaoUpdate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// LastModified is a Unix timestamp
	LastModified int64 `json:"last_modified"`

	Witnesses []string `json:"witnesses"`
}

// Verify that the LaoUpdate message is valid
func (message LaoUpdate) Verify() error {
	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("lao id is %s, should be base64URL encoded", message.ID)
	}

	// verify lao name non-empty
	if len(message.Name) == 0 {
		return errors.NewInvalidMessageFieldError("lao name is %s, should not be empty", message.Name)
	}

	// verify LastModified is positive
	if message.LastModified < 0 {
		return errors.NewInvalidMessageFieldError("last modified is %d, should be minimum 0", message.LastModified)
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
func (LaoUpdate) GetObject() string {
	return mmessage.LAOObject
}

// GetAction implements MessageData
func (LaoUpdate) GetAction() string {
	return mmessage.LAOActionUpdate
}

// NewEmpty implements MessageData
func (LaoUpdate) NewEmpty() mmessage.MessageData {
	return &LaoUpdate{}
}
