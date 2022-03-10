package messagedata

import (
	"encoding/base64"
	"golang.org/x/xerrors"
)

// ChirpNotifyAdd defines a message data
type ChirpNotifyAdd struct {
	Object  string `json:"object"`
	Action  string `json:"action"`
	ChirpId string `json:"chirp_id"`
	Channel string `json:"channel"`

	//Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// Verify verifies that the ChirpNotifyDelete message is correct
// Verify implements Verifiable
func (message ChirpNotifyAdd) Verify() error {
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
func (ChirpNotifyAdd) GetObject() string {
	return ChirpObject
}

// GetAction implements MessageData
func (ChirpNotifyAdd) GetAction() string {
	return ChirpActionNotifyAdd
}

// NewEmpty implements MessageData
func (ChirpNotifyAdd) NewEmpty() MessageData {
	return &ChirpNotifyAdd{}
}
