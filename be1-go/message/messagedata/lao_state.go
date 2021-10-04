package messagedata

import "golang.org/x/xerrors"

// LaoState defines a message data
type LaoState struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// Creation is a Unix timestamp
	Creation int64 `json:"creation"`

	// LastModified is a Unix timestamp
	LastModified int64 `json:"last_modified"`

	Organizer              string                  `json:"organizer"`
	Witnesses              []string                `json:"witnesses"`
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}

// ModificationSignature defines a modification signature of a lao state.
type ModificationSignature struct {
	Witness   string `json:"witness"`
	Signature string `json:"signature"`
}

// Verify that a LaoState message is valid
func (message LaoState) Verify(originLaoID string) error {

	laoPathID := RootPrefix + message.ID

	// Check that the message has the ID of the correct channel
	if laoPathID != originLaoID {
		return xerrors.Errorf("invalid LaoState message: invalid ID")
	}

	return nil
}
