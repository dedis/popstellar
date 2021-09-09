package messagedata

// RollCallClose ...
type RollCallClose struct {
	Object    string
	Action    string
	UpdateID  string `json:"update_id"`
	Closes    string
	ClosedAt  int `json:"closed_at"`
	Attendees []string
}
