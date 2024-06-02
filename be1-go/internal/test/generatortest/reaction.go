package generatortest

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"testing"
)

func NewReactionAddMsg(t *testing.T, sender string, senderSK kyber.Scalar, reactionCodePoint, ChirpID string,
	timestamp int64) message.Message {

	reactionAdd := messagedata.ReactionAdd{
		Object:            messagedata.ReactionObject,
		Action:            messagedata.ReactionActionAdd,
		ReactionCodepoint: reactionCodePoint,
		ChirpID:           ChirpID,
		Timestamp:         timestamp,
	}

	dataBuf, err := json.Marshal(reactionAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}

func NewReactionDeleteMsg(t *testing.T, sender string, senderSK kyber.Scalar, reactionID string,
	timestamp int64) message.Message {

	reactionDelete := messagedata.ReactionDelete{
		Object:     messagedata.ReactionObject,
		Action:     messagedata.ReactionActionDelete,
		ReactionID: reactionID,
		Timestamp:  timestamp,
	}

	dataBuf, err := json.Marshal(reactionDelete)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}
