package messagedata

import (
	"golang.org/x/xerrors"
)

// ChirpAdd defines a message data
type ChirpAdd struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	Text      string `json:"text"`
	Timestamp int64  `json:"timestamp"`
}

// Verify verifies that the ChirpAdd message is correct
func (message ChirpAdd) Verify() error {
	// verify that Timestamp is positive
	if message.Timestamp < 0 {
		return xerrors.Errorf("timestamp is %d, should be minimum 0", message.Timestamp)
	}

	return nil
}
