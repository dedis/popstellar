package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	melection2 "popstellar/internal/handler/channel/election/melection"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewElectionOpenMsg(t *testing.T, sender, lao, electionID string, openedAt int64,
	senderSK kyber.Scalar) mmessage.Message {
	electionOpen := melection2.ElectionOpen{
		Object:   channel.ElectionObject,
		Action:   channel.ElectionActionOpen,
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
	electionEnd := melection2.ElectionEnd{
		Object:          channel.ElectionObject,
		Action:          channel.ElectionActionEnd,
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

func NewElectionResultMsg(t *testing.T, sender string, questions []melection2.ElectionResultQuestion,
	senderSK kyber.Scalar) mmessage.Message {
	electionResult := melection2.ElectionResult{
		Object:    channel.ElectionObject,
		Action:    channel.ElectionActionResult,
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
		Object:    channel.ElectionObject,
		Action:    channel.VoteActionCastVote,
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
		Object:    channel.ElectionObject,
		Action:    channel.VoteActionCastVote,
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
