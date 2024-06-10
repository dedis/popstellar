package melection

import (
	"popstellar/internal/handler/messagedata"
)

// VoteCastWriteIn defines a message data
type VoteCastWriteIn struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Votes []WriteInVote `json:"votes"`
}

// WriteInVote defines a vote in a write in
type WriteInVote struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	WriteIn  string `json:"write_in"`
}

// GetObject implements MessageData
func (VoteCastWriteIn) GetObject() string {
	return messagedata.ElectionObject
}

// GetAction implements MessageData
func (VoteCastWriteIn) GetAction() string {
	return messagedata.VoteActionWriteIn
}

// NewEmpty implements MessageData
func (VoteCastWriteIn) NewEmpty() messagedata.MessageData {
	return &VoteCastVote{}
}
