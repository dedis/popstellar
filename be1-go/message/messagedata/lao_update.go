package messagedata

import "golang.org/x/xerrors"

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

// Verifiy that the LaoUpdate message is valid
func (message LaoUpdate) Verifiy(originLaoID string) error {

	laoPathID := RootPrefix + message.ID

	// Check that the message has the ID of the correct channel
	if laoPathID != originLaoID {
		return xerrors.Errorf("invalid LaoUpdate message: invalid ID")
	}

	return nil
}
