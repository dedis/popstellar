package messagedata

// LaoCreate ...
type LaoCreate struct {
	Object    string   `json:"object"`
	Action    string   `json:"action"`
	ID        string   `json:"id"`
	Name      string   `json:"name"`
	Creation  int64    `json:"creation"`
	Organizer string   `json:"organizer"`
	Witnesses []string `json:"witnesses"`
}
