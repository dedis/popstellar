package hquery

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"popstellar/internal/errors"
	"popstellar/internal/handler/query/hquery/mocks"
	"popstellar/internal/handler/query/mquery"
	mocks2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_handleQuery(t *testing.T) {
	methodHandler := mocks.NewMethodHandler(t)

	methodHandlers := make(MethodHandlers)
	methodHandlers[mquery.MethodSubscribe] = methodHandler

	queryHandler := New(methodHandlers, zerolog.New(io.Discard))

	// succeed to handled known query method without any error

	queryID := 0

	fakeSocket := mocks2.NewFakeSocket("0")
	msg := generator.NewSubscribeQuery(t, queryID, "/root")

	methodHandler.On("Handle", fakeSocket, msg).Return(&queryID, nil).Once()

	err := queryHandler.Handle(fakeSocket, msg)
	require.NoError(t, err)

	// failed to handled known query method + send error to socket

	queryID = 0
	contains := "Nop"

	fakeSocket = mocks2.NewFakeSocket("0")
	msg = generator.NewSubscribeQuery(t, queryID, "/root")

	methodHandler.On("Handle", fakeSocket, msg).Return(&queryID, errors.NewInvalidMessageFieldError(contains)).Once()

	err = queryHandler.Handle(fakeSocket, msg)
	require.Error(t, err)
	require.Contains(t, err.Error(), contains)
	require.Error(t, fakeSocket.Err)
	require.Contains(t, fakeSocket.Err.Error(), contains)

	// failed to handled query because unknown method

	queryID = 0
	contains = "unexpected method"

	fakeSocket = mocks2.NewFakeSocket("0")
	msg = generator.NewNothingQuery(t, queryID)

	err = queryHandler.Handle(fakeSocket, msg)
	require.Error(t, err)
	require.Contains(t, err.Error(), contains)

}
