package hpublish

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/method/publish/hpublish/mocks"
	"popstellar/internal/logger"
	mocks2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_Handle(t *testing.T) {
	log := logger.Logger.With().Str("test", "Handle").Logger()

	hub := mocks.NewHub(t)
	messageHandler := mocks.NewMessageHandler(t)
	fHandler := mocks.NewFederationHandler(t)
	db := mocks.NewRepository(t)

	publishHandler := New(hub, db, messageHandler, fHandler, log)

	socket := mocks2.NewFakeSocket("0")

	queryID := 0
	channelPath := "/root"
	msg := generator.NewNothingMsg(t, "sender", nil)
	publishBuf := generator.NewPublishQuery(t, queryID, channelPath, msg)

	// succeed handling the publish + notify new rumor

	messageHandler.On("Handle", channelPath, msg, false).Return(nil).Once()
	db.On("AddMessageToMyRumor", msg.MessageID).Return(thresholdMessagesByRumor, nil).Once()
	hub.On("NotifyResetRumorSender").Return(nil).Once()

	id, err := publishHandler.Handle(socket, publishBuf)
	require.Nil(t, id)
	require.NoError(t, err)

	// succeed handling the publish but no notify rumor

	messageHandler.On("Handle", channelPath, msg, false).Return(nil).Once()
	db.On("AddMessageToMyRumor", msg.MessageID).Return(thresholdMessagesByRumor-1, nil).Once()

	id, err = publishHandler.Handle(socket, publishBuf)
	require.Nil(t, id)
	require.NoError(t, err)

	// succeed handling the publish but no rumor because federation channel

	channelPath = "/root/laoID/federation"
	publishBuf = generator.NewPublishQuery(t, queryID, channelPath, msg)

	//messageHandler.On("Handle", channelPath, msg, false).Return(nil).Once()
	fHandler.On("Handle", channelPath, msg, socket).Return(nil).Once()

	id, err = publishHandler.Handle(socket, publishBuf)
	require.Nil(t, id)
	require.NoError(t, err)
}
