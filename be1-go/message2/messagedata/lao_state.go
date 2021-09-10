package messagedata

// LaoState ...
type LaoState struct {
	Object                 string                  `json:"object"`
	Action                 string                  `json:"action"`
	ID                     string                  `json:"id"`
	Name                   string                  `json:"name"`
	Creation               int                     `json:"creation"`
	LastModified           int                     `json:"last_modified"`
	Organizer              string                  `json:"organizer"`
	Witnesses              []string                `json:"witnesses"`
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}

// ModificationSignature ...
type ModificationSignature struct {
	Witness   string `json:"witness"`
	Signature string `json:"signature"`
}
