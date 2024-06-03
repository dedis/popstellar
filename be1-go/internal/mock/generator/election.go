package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"testing"
)

func NewElectionOpenMsg(t *testing.T, sender, lao, election string, openedAt int64,
	senderSK kyber.Scalar) message.Message {
	electionOpen := messagedata.ElectionOpen{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionOpen,
		Lao:      lao,
		Election: election,
		OpenedAt: openedAt,
	}

	electionOpenBuf, err := json.Marshal(electionOpen)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionOpenBuf)

	return msg
}

func NewElectionCloseMsg(t *testing.T, sender, lao, election, registeredVotes string, openedAt int64,
	senderSK kyber.Scalar) message.Message {
	electionEnd := messagedata.ElectionEnd{
		Object:          messagedata.ElectionObject,
		Action:          messagedata.ElectionActionEnd,
		Lao:             lao,
		Election:        election,
		CreatedAt:       openedAt,
		RegisteredVotes: registeredVotes,
	}

	electionEndBuf, err := json.Marshal(electionEnd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionEndBuf)

	return msg
}

func NewElectionResultMsg(t *testing.T, sender string, questions []messagedata.ElectionResultQuestion,
	senderSK kyber.Scalar) message.Message {
	electionResult := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: questions,
	}

	electionResultBuf, err := json.Marshal(electionResult)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, electionResultBuf)

	return msg
}

func NewVoteCastVoteIntMsg(t *testing.T, sender, lao, election string, createdAt int64, votes []VoteInt,
	senderSK kyber.Scalar) message.Message {
	castVote := VoteCastVoteInt{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.VoteActionCastVote,
		Lao:       lao,
		Election:  election,
		CreatedAt: createdAt,
		Votes:     votes,
	}

	castVoteBuf, err := json.Marshal(castVote)
	require.NoError(t, err)

	return newMessage(t, sender, senderSK, castVoteBuf)
}

func NewVoteCastVoteStringMsg(t *testing.T, sender, lao, election string, createdAt int64, votes []VoteString,
	senderSK kyber.Scalar) message.Message {
	castVote := VoteCastVoteString{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.VoteActionCastVote,
		Lao:       lao,
		Election:  election,
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