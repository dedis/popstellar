package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
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

	chirpNotifyChannelID, err := getGeneralChirpsChannel(channelID)
	require.NoError(t, err)

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

	chirpNotifyChannelID, err := getGeneralChirpsChannel(channelID)
	require.NoError(t, err)

	subs.AddChannel(chirpNotifyChannelID)

	mockRepo.On("StoreMessage", chirpNotifyChannelID, mock.AnythingOfType("message.Message")).Return(nil)

	return msg
}

func getGeneralChirpsChannel(channelID string) (string, error) {
	channelID, _ = strings.CutPrefix(channelID, "/")
	splitChannelID := strings.Split(channelID, "/")

	if len(splitChannelID) != 4 || splitChannelID[0] != "root" || splitChannelID[2] != "social" {
		return "", xerrors.Errorf("invalid channel")
	}

	generalChirpsChannelID := "/root/" + splitChannelID[1] + "/social/chirps"

	return generalChirpsChannelID, nil
}
