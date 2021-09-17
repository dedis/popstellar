package messagedata

// VoteCastVote ...
type VoteCastVote struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	Lao       string `json:"lao"`
	Election  string `json:"election"`
	CreatedAt int64  `json:"created_at"`
	Votes     []Vote `json:"votes"`
}

// Vote ...
type Vote struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	Vote     []int  `json:"vote"`
}
