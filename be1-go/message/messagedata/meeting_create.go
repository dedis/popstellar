package messagedata

import "encoding/json"

// MeetingCreate ...
type MeetingCreate struct {
	Object   string          `json:"object"`
	Action   string          `json:"action"`
	ID       string          `json:"id"`
	Name     string          `json:"name"`
	Creation int64           `json:"creation"`
	Location string          `json:"location"`
	Start    int64           `json:"start"`
	End      int64           `json:"end"`
	Extra    json.RawMessage `json:"extra"`
}
