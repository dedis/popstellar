package hrumorstate

import (
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumorstate/hrumorstate/mocks"
	"popstellar/internal/logger"
	mocks2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_Handle(t *testing.T) {

	log := logger.Logger.With().Str("test", "Handle").Logger()

	queries := mocks.NewQueries(t)
	db := mocks.NewRepository(t)
	sockets := mocks.NewSockets(t)
	fakeSocket := mocks2.NewFakeSocket("0")

	rumorStateHandler := New(queries, sockets, db, log)

	sender1 := "sender1"
	sender2 := "sender2"

	timestamp1 := make(mrumor.RumorTimestamp)
	timestamp1[sender1] = 0
	timestamp2 := make(mrumor.RumorTimestamp)
	timestamp2[sender2] = 0
	timestamp3 := make(mrumor.RumorTimestamp)
	timestamp3[sender1] = 1

	rumor1, _ := generator.NewRumorQuery(t, 1, sender1, 0, timestamp1, make(map[string][]mmessage.Message))
	rumor2, _ := generator.NewRumorQuery(t, 2, sender2, 0, timestamp2, make(map[string][]mmessage.Message))
	rumor3, _ := generator.NewRumorQuery(t, 3, sender1, 1, timestamp3, make(map[string][]mmessage.Message))

	rumors := []mrumor.Rumor{rumor3, rumor1, rumor2}

	db.On("GetAllRumors").Return(rumors, nil).Once()

	_, rumorStateBuf1 := generator.NewRumorStateQuery(t, 4, timestamp1)

	id, err := rumorStateHandler.Handle(fakeSocket, rumorStateBuf1)
	require.NoError(t, err)
	require.Nil(t, id)

	expected := []mrumor.Rumor{rumor3, rumor2}

	require.Equal(t, fakeSocket.ResultID, 4)
	require.Equal(t, expected, fakeSocket.Rumors)

	db.On("GetAllRumors").Return(rumors, nil).Once()

	_, rumorStateBuf2 := generator.NewRumorStateQuery(t, 4, timestamp2)
	id, err = rumorStateHandler.Handle(fakeSocket, rumorStateBuf2)
	require.NoError(t, err)
	require.Nil(t, id)

	expected = []mrumor.Rumor{rumor1, rumor3}

	require.Equal(t, fakeSocket.ResultID, 4)
	require.Equal(t, expected, fakeSocket.Rumors)

	db.On("GetAllRumors").Return(rumors, nil).Once()

	_, rumorStateBuf3 := generator.NewRumorStateQuery(t, 4, timestamp3)
	id, err = rumorStateHandler.Handle(fakeSocket, rumorStateBuf3)
	require.NoError(t, err)
	require.Nil(t, id)

	expected = []mrumor.Rumor{rumor2}

	require.Equal(t, fakeSocket.ResultID, 4)
	require.Equal(t, expected, fakeSocket.Rumors)

}

func Test_SendRumorState(t *testing.T) {
	log := logger.Logger.With().Str("test", "SendRumorState").Logger()

	queries := mocks.NewQueries(t)
	db := mocks.NewRepository(t)
	sockets := mocks.NewSockets(t)

	rumorStateHandler := New(queries, sockets, db, log)

	timestamp := make(mrumor.RumorTimestamp)
	timestamp["sender"] = 0

	db.On("GetRumorTimestamp").Return(timestamp, nil).Once()
	queries.On("GetNextID").Return(1).Once()
	queries.On("AddRumorState", 1).Return(nil).Once()
	sockets.On("SendToRandom", mock.AnythingOfType("[]uint8")).Once()

	err := rumorStateHandler.SendRumorState()
	require.NoError(t, err)
}
