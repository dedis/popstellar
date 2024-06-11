package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	mchirp2 "popstellar/internal/handler/channel/chirp/mchirp"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewChirpAddMsg(t *testing.T, sender string, senderSK kyber.Scalar, timestamp int64) mmessage.Message {

	chirpAdd := mchirp2.ChirpAdd{
		Object:    channel.ChirpObject,
		Action:    channel.ChirpActionAdd,
		Text:      "just a chirp",
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(chirpAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}

func NewChirpDeleteMsg(t *testing.T, sender string, senderSK kyber.Scalar, chirpID string,
	timestamp int64) mmessage.Message {

	chirpAdd := mchirp2.ChirpDelete{
		Object:    channel.ChirpObject,
		Action:    channel.ChirpActionDelete,
		ChirpID:   chirpID,
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(chirpAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}
