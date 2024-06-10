package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/messagedata/melection"
	"popstellar/internal/message/mmessage"
	"testing"
)

func NewElectionOpenMsg(t *testing.T, sender, lao, electionID string, openedAt int64,
	senderSK kyber.Scalar) mmessage.Message {
	electionOpen := melection.ElectionOpen{
		Object:   mmessage.ElectionObject,
		Action:   mmessage.ElectionActionOpen,
		Lao:      lao,
		Election: electionID,
		OpenedAt: openedAt,
	}

	electionOpenBuf, err := json.Marshal(electionOpen)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionOpenBuf)

	return msg
}

func NewElectionCloseMsg(t *testing.T, sender, lao, electionID, registeredVotes string, openedAt int64,
	senderSK kyber.Scalar) mmessage.Message {
	electionEnd := melection.ElectionEnd{
		Object:          mmessage.ElectionObject,
		Action:          mmessage.ElectionActionEnd,
		Lao:             lao,
		Election:        electionID,
		CreatedAt:       openedAt,
		RegisteredVotes: registeredVotes,
	}

	electionEndBuf, err := json.Marshal(electionEnd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionEndBuf)

	return msg
}

func NewElectionResultMsg(t *testing.T, sender string, questions []melection.ElectionResultQuestion,
	senderSK kyber.Scalar) mmessage.Message {
	electionResult := melection.ElectionResult{
		Object:    mmessage.ElectionObject,
		Action:    mmessage.ElectionActionResult,
		Questions: questions,
	}

	electionResultBuf, err := json.Marshal(electionResult)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionResultBuf)

	return msg
}

func NewVoteCastVoteIntMsg(t *testing.T, sender, lao, electionID string, createdAt int64, votes []VoteInt,
	senderSK kyber.Scalar) mmessage.Message {
	castVote := VoteCastVoteInt{
		Object:    mmessage.ElectionObject,
		Action:    mmessage.VoteActionCastVote,
		Lao:       lao,
		Election:  electionID,
		CreatedAt: createdAt,
		Votes:     votes,
	}

	castVoteBuf, err := json.Marshal(castVote)
	require.NoError(t, err)

	return newMessage(t, sender, senderSK, castVoteBuf)
}

func NewVoteCastVoteStringMsg(t *testing.T, sender, lao, electionID string, createdAt int64, votes []VoteString,
	senderSK kyber.Scalar) mmessage.Message {
	castVote := VoteCastVoteString{
		Object:    mmessage.ElectionObject,
		Action:    mmessage.VoteActionCastVote,
		Lao:       lao,
		Election:  electionID,
		CreatedAt: createdAt,
		Votes:     votes,
	}

	castVoteBuf, err := json.Marshal(castVote)
	require.NoError(t, err)

	return newMessage(t, sender, senderSK, castVoteBuf)
}

type VoteCastVoteInt struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Votes []VoteInt `json:"votes"`
}

type VoteInt struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	Vote     int    `json:"vote"`
}

type VoteCastVoteString struct {
	Object   string `json:"object"`
	Action   string `json:"action"`
	Lao      string `json:"lao"`
	Election string `json:"election"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Votes []VoteString `json:"votes"`
}

type VoteString struct {
	ID       string `json:"id"`
	Question string `json:"question"`
	Vote     string `json:"vote"`
}
