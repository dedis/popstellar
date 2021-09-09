package messagedata

// RollCallReOpen ...
type RollCallReOpen struct {
	Object   string
	Action   string
	UpdateID string `json:"update_id"`
	Opens    string
	OpenedAt int `json:"opened_at"`
}
