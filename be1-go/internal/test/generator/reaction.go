package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	mreaction2 "popstellar/internal/handler/channel/reaction/mreaction"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewReactionAddMsg(t *testing.T, sender string, senderSK kyber.Scalar, reactionCodePoint, ChirpID string,
	timestamp int64) mmessage.Message {

	reactionAdd := mreaction2.ReactionAdd{
		Object:            channel.ReactionObject,
		Action:            channel.ReactionActionAdd,
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
	timestamp int64) mmessage.Message {

	reactionDelete := mreaction2.ReactionDelete{
		Object:     channel.ReactionObject,
		Action:     channel.ReactionActionDelete,
		ReactionID: reactionID,
		Timestamp:  timestamp,
	}

	dataBuf, err := json.Marshal(reactionDelete)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}
