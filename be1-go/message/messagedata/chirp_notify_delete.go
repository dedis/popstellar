package messagedata

import (
	"encoding/base64"
	"golang.org/x/xerrors"
)

type ChirpNotifyDelete struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	ChirpId   string `json:"chirp_id"`
	Channel   string `json:"channel"`
	Timestamp int64  `json:"timestamp"`
}

// Verify verifies that the ChirpNotifyDelete message is correct
// Verify implements VerifiableMessageData
func (message ChirpNotifyDelete) Verify() error {
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

//GetObject implements MessageData
func (ChirpNotifyDelete) GetObject() string {
	return ChirpObject

}

//GetAction implements MessageData
func (ChirpNotifyDelete) GetAction() string {
	return ChirpActionNotifyDelete
}

//NewEmpty implements MessageData
func (ChirpNotifyDelete) NewEmpty() MessageData {
	return &ChirpNotifyDelete{}

}
