package messagedata

// RollCallClose ...
type RollCallClose struct {
	Object    string   `json:"object"`
	Action    string   `json:"action"`
	UpdateID  string   `json:"update_id"`
	Closes    string   `json:"closes"`
	ClosedAt  int64    `json:"closed_at"`
	Attendees []string `json:"attendees"`
}
