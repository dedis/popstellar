package messagedata

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

// GetObject implements MessageData
func (LaoState) GetObject() string {
	return LAOObject
}

// GetAction implements MessageData
func (LaoState) GetAction() string {
	return LAOActionState
}

// NewEmpty implements MessageData
func (LaoState) NewEmpty() MessageData {
	return &LaoState{}
}
