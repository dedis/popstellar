package messagedata

// VoteCastVote defines a message data
type VoteCastVote struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Votes []Vote `json:"votes"`
}

// Vote defines a vote of a cast vote
type Vote struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	Vote     []int  `json:"vote"`
}

// GetObject implements MessageData
func (VoteCastVote) GetObject() string {
	return ElectionObject
}

// GetAction implements MessageData
func (VoteCastVote) GetAction() string {
	return VoteActionCastVote
}

// NewEmpty implements MessageData
func (VoteCastVote) NewEmpty() MessageData {
	return &VoteCastVote{}
}
