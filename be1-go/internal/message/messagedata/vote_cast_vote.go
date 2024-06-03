package messagedata

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message/answer"
	"strings"
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
	Vote     interface{} // Can be int or string
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

	err := json.Unmarshal(b, &vTemp)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal vote")
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
	case string:
		*v = Vote(vTemp)
	default:
		return answer.NewErrorf(-4, "invalid vote type, should be int or string but was %v", t)
	}

	return nil
}

func (message VoteCastVote) Verify(electionPath string) error {
	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.Lao)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode lao: %v", err)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.Election)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode election: %v", err)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(electionPath, RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return errors.NewInvalidMessageFieldError("failed to split channel: %v", electionPath)
	}

	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if message.Lao != laoID {
		return errors.NewInvalidMessageFieldError("lao id is not the same as the channel")
	}

	// verify if election id is the same as the channel
	if message.Election != electionID {
		return errors.NewInvalidMessageFieldError("election id is not the same as the channel")
	}

	return nil
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
