package messagedata

// LaoState ...
type LaoState struct {
	Object                 string
	Action                 string
	ID                     string
	Name                   string
	Creation               int
	LastModified           int `json:"last_modified"`
	Organizer              string
	Witnesses              []string
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}

// ModificationSignature ...
type ModificationSignature struct {
	Witness   string
	Signature string
}
