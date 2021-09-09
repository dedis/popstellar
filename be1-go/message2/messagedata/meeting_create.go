package messagedata

import "encoding/json"

// MeetingCreate ...
type MeetingCreate struct {
	Object   string
	Action   string
	ID       string
	Name     string
	Creation int
	Location string
	Start    int
	End      int
	Extra    json.RawMessage
}
