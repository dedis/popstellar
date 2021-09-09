package messagedata

// VoteCastVote ...
type VoteCastVote struct {
	Object    string
	Action    string
	Lao       string
	Election  string
	CreatedAt int `json:"created_at"`
	Votes     []Vote
}

// Vote ...
type Vote struct {
	ID       string
	Question string
	Vote     []int
}
