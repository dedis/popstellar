package messagedata

// RollCallOpen ...
type RollCallOpen struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	UpdateID string `json:"update_id"`
	Opens    string `json:"opens"`
	OpenedAt int64  `json:"opened_at"`
}
