package messagedata

import "encoding/json"

// MeetingCreate ...
type MeetingCreate struct {
	Object   string          `json:"object"`
	Action   string          `json:"action"`
	ID       string          `json:"id"`
	Name     string          `json:"name"`
	Creation int             `json:"creation"`
	Location string          `json:"location"`
	Start    int             `json:"start"`
	End      int             `json:"end"`
	Extra    json.RawMessage `json:"extra"`
}
