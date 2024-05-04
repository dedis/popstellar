package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func NewChirpAddMsg(t *testing.T, channelID string, sender string, senderPK kyber.Scalar, timestamp int64,
	mockRepo *database.MockRepository, isError bool) message.Message {

	chirpAdd := messagedata.ChirpAdd{
		Object:    messagedata.ChirpObject,
		Action:    messagedata.ChirpActionAdd,
		Text:      "just a chirp",
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(chirpAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderPK, dataBuf)

	if mockRepo == nil {
		return msg
	}

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	if isError {
		return msg
	}

	mockRepo.On("StoreMessage", channelID, msg).Return(nil)

	chirpNotifyChannelID, errAnswer := getGeneralChirpsChannel(channelID)
	require.Nil(t, errAnswer)

	subs.AddChannel(chirpNotifyChannelID)

	mockRepo.On("StoreMessage", chirpNotifyChannelID, mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func NewChirpDeleteMsg(t *testing.T, channelID string, sender string, senderPK kyber.Scalar, chirpID string,
	timestamp int64, mockRepo *database.MockRepository, isError bool) message.Message {

	chirpAdd := messagedata.ChirpDelete{
		Object:    messagedata.ChirpObject,
		Action:    messagedata.ChirpActionDelete,
		ChirpID:   chirpID,
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(chirpAdd)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderPK, dataBuf)

	if mockRepo == nil {
		return msg
	}

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	if isError {
		return msg
	}

	mockRepo.On("HasMessage", chirpID).Return(true, nil)
	mockRepo.On("StoreMessage", channelID, msg).Return(nil)

	chirpNotifyChannelID, errAnswer := getGeneralChirpsChannel(channelID)
	require.Nil(t, errAnswer)

	subs.AddChannel(chirpNotifyChannelID)

	mockRepo.On("StoreMessage", chirpNotifyChannelID, mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func newMessage(t *testing.T, sender string, senderPK kyber.Scalar, data []byte) message.Message {
	data64 := base64.URLEncoding.EncodeToString(data)

	signature64 := "Signature"

	if senderPK != nil {
		signatureBuf, err := schnorr.Sign(crypto.Suite, senderPK, data)
		require.NoError(t, err)

		signature64 = base64.URLEncoding.EncodeToString(signatureBuf)
	}

	messageID64 := messagedata.Hash(data64, signature64)

	return message.Message{
		Data:              data64,
		Sender:            sender,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: nil,
	}
}
