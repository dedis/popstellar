package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
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
