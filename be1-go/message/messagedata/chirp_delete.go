package messagedata

import (
	"encoding/base64"
	"golang.org/x/xerrors"
)

// ChirpDelete defines a message data
type ChirpDelete struct {
	Object  string `json:"object"`
	Action  string `json:"action"`
	ChirpId string `json:"chirp_id"`

	//Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify implements Verifiable. It verifies that the ChirpDelete message is correct
func (message ChirpDelete) Verify() error {
	// verify that Timestamp is positive
	if message.Timestamp < 0 {
		return xerrors.Errorf("timestamp is %d, should be minimum 0", message.Timestamp)
	}

	// verify that the chirp id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ChirpId)
	if err != nil {
		return xerrors.Errorf("chirp id is %s, should be base64URL encoded", message.ChirpId)
	}

	return nil
}

// GetObject implements MessageData
func (ChirpDelete) GetObject() string {
	return ChirpObject
}

// GetAction implements MessageData
func (ChirpDelete) GetAction() string {
	return ChirpActionDelete
}

// NewEmpty implements MessageData
func (ChirpDelete) NewEmpty() MessageData {
	return &ChirpDelete{}
}
