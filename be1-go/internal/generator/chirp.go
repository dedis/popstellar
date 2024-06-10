package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/messagedata/mchirp"
	"popstellar/internal/message/mmessage"
	"testing"
)

func NewChirpAddMsg(t *testing.T, sender string, senderSK kyber.Scalar, timestamp int64) mmessage.Message {

	chirpAdd := mchirp.ChirpAdd{
		Object:    messagedata.ChirpObject,
		Action:    messagedata.ChirpActionAdd,
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

	chirpAdd := mchirp.ChirpDelete{
		Object:    messagedata.ChirpObject,
		Action:    messagedata.ChirpActionDelete,
		ChirpID:   chirpID,
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(chirpAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, dataBuf)

	return msg
}
