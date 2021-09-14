package messagedata

// RollCallCreate ...
type RollCallCreate struct {
	Object        string `json:"object"`
	Action        string `json:"action"`
	ID            string `json:"id"`
	Name          string `json:"name"`
	Creation      int64  `json:"creation"`
	ProposedStart int64  `json:"proposed_start"`
	ProposedEnd   int64  `json:"proposed_end"`
	Location      string `json:"location"`
	Description   string `json:"description"`
}
