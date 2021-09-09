package messagedata

// RollCallCreate ...
type RollCallCreate struct {
	Object        string
	Action        string
	ID            string
	Name          string
	Creation      int
	ProposedStart int `json:"proposed_start"`
	ProposedEnd   int `json:"proposed_end"`
	Location      string
	Description   string
}
