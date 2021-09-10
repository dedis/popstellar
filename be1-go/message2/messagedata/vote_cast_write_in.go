package messagedata

// VoteCastWriteIn ...
type VoteCastWriteIn struct {
	Object    string        `json:"object"`
	Action    string        `json:"action"`
	Lao       string        `json:"lao"`
	Election  string        `json:"election"`
	CreatedAt int           `json:"created_at"`
	Votes     []WriteInVote `json:"votes"`
}

// WriteInVote ...
type WriteInVote struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	WriteIn  string `json:"write_in"`
}
