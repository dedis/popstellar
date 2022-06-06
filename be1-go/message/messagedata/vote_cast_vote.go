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

// Vote defines a vote of a cast vote, Vote.Vote can be an int or a string
type Vote struct {
	ID       string
	Question string
	Vote     interface{}
}

// tempVote is used to prevent the infinite loop when unmarshalling
type tempVote struct {
	ID       string      `json:"id"`
	Question string      `json:"question"`
	Vote     interface{} `json:"vote"`
}

// UnmarshalJSON solves that Vote.Vote can be int or string
func (v *Vote) UnmarshalJSON(b []byte) error {
	// unmarshalling into a tempVote
	var vTemp tempVote
	if err := json.Unmarshal(b, &vTemp); err != nil {
		return err
	}

	// checking that Vote.Vote is either an int or a string
	switch t := vTemp.Vote.(type) {
	// json unmarshalls numbers only into float64, we have to make additional checks
	case float64:
		i := int(t)
		if float64(i) != t {
			return answer.NewErrorf(-4, "invalid vote type, should be int but was float")
		}
		*v = Vote{vTemp.ID, vTemp.Question, i}
		return nil
	case string:
		*v = Vote(vTemp)
		return nil
	default:
		return answer.NewErrorf(-4, "invalid vote type, should be int or string but was %v", t)
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
