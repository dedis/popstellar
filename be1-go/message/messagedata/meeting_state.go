package messagedata

import "encoding/json"

// MeetingState ...
type MeetingState struct {
	Object                 string                  `json:"object"`
	Action                 string                  `json:"action"`
	ID                     string                  `json:"id"`
	Name                   string                  `json:"name"`
	Creation               int64                   `json:"creation"`
	LastModified           int64                   `json:"last_modified"`
	Location               string                  `json:"location"`
	Start                  int64                   `json:"start"`
	End                    int64                   `json:"end"`
	Extra                  json.RawMessage         `json:"extra"`
	ModificationID         string                  `json:"modification_id"`
	ModificationSignatures []ModificationSignature `json:"modification_signatures"`
}
