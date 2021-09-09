package messagedata

import "encoding/json"

// MeetingState ...
type MeetingState struct {
	Object                 string
	Action                 string
	ID                     string
	Name                   string
	Creation               int
	LastModified           int `json:"last_modified"`
	Location               string
	Start                  int
	End                    int
	Extra                  json.RawMessage
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}
