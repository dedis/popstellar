package messagedata

// VoteCastWriteIn ...
type VoteCastWriteIn struct {
	Object    string
	Action    string
	Lao       string
	Election  string
	CreatedAt int `json:"created_at"`
	Votes     []WriteInVote
}

// WriteInVote ...
type WriteInVote struct {
	ID       string
	Question string
	WriteIn  string `json:"write_in"`
}
