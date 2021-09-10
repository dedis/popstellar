package messagedata

// LaoUpdate ...
type LaoUpdate struct {
	Object       string   `json:"object"`
	Action       string   `json:"action"`
	ID           string   `json:"id"`
	Name         string   `json:"name"`
	LastModified int      `json:"last_modified"`
	Witnesses    []string `json:"witnesses"`
}
