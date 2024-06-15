package hrumor

import (
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/hrumor/mocks"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/logger"
	mocks2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_Handle(t *testing.T) {
	log := logger.Logger.With().Str("test", "Handle").Logger()

	fakeSocket := mocks2.NewFakeSocket("0")

	queries := mocks.NewQueries(t)
	sockets := mocks.NewSockets(t)
	messageHandler := mocks.NewMessageHandler(t)
	db := mocks.NewRepository(t)

	rumorHandler := New(queries, sockets, db, messageHandler, log)

	sender := "sender"

	timestamp0 := make(mrumor.RumorTimestamp)
	timestamp1 := make(mrumor.RumorTimestamp)
	timestamp1[sender] = 0
	timestamp2 := make(mrumor.RumorTimestamp)
	timestamp2[sender] = 1

	rumor0, rumorBuf0 := generator.NewRumorQuery(t, 0, sender, 0, timestamp0, nil)
	rumor1, rumorBuf1 := generator.NewRumorQuery(t, 1, sender, 1, timestamp1, nil)
	rumor2, rumorBuf2 := generator.NewRumorQuery(t, 2, sender, 2, timestamp2, nil)

	// rumor1 is not valid but stored inside the buffer

	db.On("CheckRumor", rumor1.Params.SenderID, rumor1.Params.RumorID, rumor1.Params.Timestamp).
		Return(false, false, nil).Once()

	_, err := rumorHandler.Handle(fakeSocket, rumorBuf1)
	require.NoError(t, err)
	require.Equal(t, fakeSocket.ResultID, rumor1.ID)

	// rumor1 is not valid but rejected because already inside the buffer

	db.On("CheckRumor", rumor1.Params.SenderID, rumor1.Params.RumorID, rumor1.Params.Timestamp).
		Return(false, false, nil).Once()

	_, err = rumorHandler.Handle(fakeSocket, rumorBuf1)
	require.Error(t, err)
	require.Contains(t, err.Error(), "rumor sender:1 is already inside the buffer")

	// rumor2 is not valid but stored inside the buffer

	db.On("CheckRumor", rumor2.Params.SenderID, rumor2.Params.RumorID, rumor2.Params.Timestamp).
		Return(false, false, nil).Once()

	_, err = rumorHandler.Handle(fakeSocket, rumorBuf2)
	require.NoError(t, err)
	require.Equal(t, fakeSocket.ResultID, rumor2.ID)

	// rumor0 is valid then rumor1 and rumor2 are handled

	db.On("CheckRumor", rumor0.Params.SenderID, rumor0.Params.RumorID, rumor0.Params.Timestamp).
		Return(true, false, nil).Once()
	queries.On("GetNextID").Return(rumor0.ID).Once()
	queries.On("AddRumor", rumor0.ID, rumor0).Return(nil).Once()
	sockets.On("SendRumor", fakeSocket, rumor0.Params.SenderID, rumor0.Params.RumorID, rumorBuf0).Once()
	db.On("StoreRumor", rumor0.Params.RumorID, rumor0.Params.SenderID,
		mock.AnythingOfType("map[string][]mmessage.Message"), []string{}).Return(nil).Once()
	db.On("GetUnprocessedMessagesByChannel").Return(nil, nil).Once()

	state0 := make(mrumor.RumorTimestamp)
	state0[sender] = rumor0.Params.RumorID
	db.On("GetRumorTimestamp").Return(state0, nil).Once()

	db.On("CheckRumor", rumor1.Params.SenderID, rumor1.Params.RumorID, rumor1.Params.Timestamp).
		Return(true, false, nil).Once()
	queries.On("GetNextID").Return(rumor1.ID).Once()
	queries.On("AddRumor", rumor1.ID, rumor1).Return(nil).Once()
	sockets.On("SendRumor", nil, rumor1.Params.SenderID, rumor1.Params.RumorID, rumorBuf1).Once()
	db.On("StoreRumor", rumor1.Params.RumorID, rumor1.Params.SenderID,
		mock.AnythingOfType("map[string][]mmessage.Message"), []string{}).Return(nil).Once()
	db.On("GetUnprocessedMessagesByChannel").Return(nil, nil).Once()

	state1 := make(mrumor.RumorTimestamp)
	state1[sender] = rumor1.Params.RumorID
	db.On("GetRumorTimestamp").Return(state1, nil).Once()

	db.On("CheckRumor", rumor2.Params.SenderID, rumor2.Params.RumorID, rumor2.Params.Timestamp).
		Return(true, false, nil).Once()
	queries.On("GetNextID").Return(rumor2.ID).Once()
	queries.On("AddRumor", rumor2.ID, rumor2).Return(nil).Once()
	sockets.On("SendRumor", nil, rumor2.Params.SenderID, rumor2.Params.RumorID, rumorBuf2).Once()
	db.On("StoreRumor", rumor2.Params.RumorID, rumor2.Params.SenderID,
		mock.AnythingOfType("map[string][]mmessage.Message"), []string{}).Return(nil).Once()
	db.On("GetUnprocessedMessagesByChannel").Return(nil, nil).Once()

	state2 := make(mrumor.RumorTimestamp)
	state2[sender] = rumor2.Params.RumorID
	db.On("GetRumorTimestamp").Return(state2, nil).Once()

	id, err := rumorHandler.Handle(fakeSocket, rumorBuf0)
	require.NoError(t, err)
	require.Nil(t, id)

	// rumor0 is rejected because it was already handled and stored inside the database

	db.On("CheckRumor", rumor0.Params.SenderID, rumor0.Params.RumorID, rumor0.Params.Timestamp).Return(false, true, nil).Once()

	id, err = rumorHandler.Handle(fakeSocket, rumorBuf0)
	require.Error(t, err)
	require.Contains(t, err.Error(), fmt.Sprintf("rumor %s:%d already exists", rumor0.Params.SenderID, rumor0.Params.RumorID))
	require.Equal(t, *id, rumor0.ID)
}

func Test_tryHandlingMessagesByChannel(t *testing.T) {
	log := logger.Logger.With().Str("test", "Test_tryHandlingMessagesByChannel").Logger()

	queries := mocks.NewQueries(t)
	sockets := mocks.NewSockets(t)
	messageHandler := mocks.NewMessageHandler(t)
	db := mocks.NewRepository(t)

	rumorHandler := New(queries, sockets, db, messageHandler, log)

	root := "/root"
	lao1 := "/root/lao1"

	msg0 := mmessage.Message{MessageID: "0"}
	msg1 := mmessage.Message{MessageID: "1"}
	msg2 := mmessage.Message{MessageID: "2"}
	msg3 := mmessage.Message{MessageID: "3"}

	messagesByChannel := make(map[string][]mmessage.Message)
	messagesByChannel[root] = []mmessage.Message{msg0, msg1}
	messagesByChannel[lao1] = []mmessage.Message{msg2, msg3}

	expectedProcessed := []string{msg0.MessageID, msg2.MessageID}

	expectedUnprocessed := make(map[string][]mmessage.Message)
	expectedUnprocessed[root] = []mmessage.Message{msg1}
	expectedUnprocessed[lao1] = []mmessage.Message{msg3}

	messageHandler.On("Handle", root, msg0, true).Return(nil).Once()
	messageHandler.On("Handle", root, msg1, true).Return(errors.NewInvalidMessageFieldError("Nop")).Times(maxRetry)
	messageHandler.On("Handle", lao1, msg2, true).Return(nil).Once()
	messageHandler.On("Handle", lao1, msg3, true).Return(errors.NewInvalidMessageFieldError("Nop")).Times(maxRetry)

	processed := rumorHandler.tryHandlingMessagesByChannel(messagesByChannel)

	require.Equal(t, expectedProcessed, processed)

	for channelPath, messages := range messagesByChannel {
		_, ok := expectedUnprocessed[channelPath]
		require.True(t, ok)
		require.Equal(t, expectedUnprocessed[channelPath], messages)
	}
}
