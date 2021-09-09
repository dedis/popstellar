package messagedata

// LaoUpdate ...
type LaoUpdate struct {
	Object       string
	Action       string
	ID           string
	Name         string
	LastModified int `json:"last_modified"`
	Witnesses    []string
}
