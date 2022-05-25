package messagedata

import (
	"encoding/json"
	"popstellar/message/answer"
)

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
	ID       string      `json:"id"`
	Question string      `json:"question"`
	Vote     interface{} `json:"vote"`
}

type raw struct {
	ID       string      `json:"id"`
	Question string      `json:"question"`
	Vote     interface{} `json:"vote"`
}

// UnmarshalJSON solves that Vote.Vote can be int or string
func (v *Vote) UnmarshalJSON(b []byte) error {
	var vTemp raw
	if err := json.Unmarshal(b, &vTemp); err != nil {
		return err
	}

	switch vTemp.Vote.(type) {
	case float64:
		f, _ := vTemp.Vote.(float64)
		i := int(f)
		*v = Vote{vTemp.ID, vTemp.Question, i}
		return nil
	case string:
		*v = Vote(vTemp)
		return nil
	default:
		return answer.NewErrorf(-4, "invalid vote type, should be int or string but was %v", vTemp.Vote)
	}
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
