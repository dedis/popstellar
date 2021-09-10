package messagedata

import "encoding/json"

// MeetingState ...
type MeetingState struct {
	Object                 string                  `json:"object"`
	Action                 string                  `json:"action"`
	ID                     string                  `json:"id"`
	Name                   string                  `json:"name"`
	Creation               int                     `json:"creation"`
	LastModified           int                     `json:"last_modified"`
	Location               string                  `json:"location"`
	Start                  int                     `json:"start"`
	End                    int                     `json:"end"`
	Extra                  json.RawMessage         `json:"extra"`
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}
