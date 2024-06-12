package hcatchup

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"io"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/catchup/hcatchup/mocks"
	mocks2 "popstellar/internal/network/socket/mocks"
	generator2 "popstellar/internal/test/generator"
	"testing"
)

func Test_handleCatchUp(t *testing.T) {
	db := mocks.NewRepository(t)

	handler := New(db, zerolog.New(io.Discard))

	type input struct {
		name     string
		socket   mocks2.FakeSocket
		ID       int
		message  []byte
		expected []mmessage.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully catchup 4 messages on a channel

	fakeSocket := mocks2.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"
	messagesToCatchUp := []mmessage.Message{
		generator2.NewNothingMsg(t, "sender1", nil),
		generator2.NewNothingMsg(t, "sender2", nil),
		generator2.NewNothingMsg(t, "sender3", nil),
		generator2.NewNothingMsg(t, "sender4", nil),
	}

	db.On("GetAllMessagesFromChannel", channel).Return(messagesToCatchUp, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator2.NewCatchupQuery(t, ID, channel),
		expected: messagesToCatchUp,
		isError:  false,
	})

	// Test 2: failed to catchup because DB is disconnected

	fakeSocket = mocks2.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	db.On("GetAllMessagesFromChannel", channel).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator2.NewCatchupQuery(t, ID, channel),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, err := handler.Handle(&arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err, arg.contains)
				require.NotNil(t, id)
				require.Equal(t, arg.ID, *id)
			} else {
				require.NoError(t, err)
				require.Equal(t, arg.expected, arg.socket.Res)
			}
		})
	}
}
