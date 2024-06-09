package catchup

import (
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/handler/method/catchup/mocks"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"testing"
)

func Test_handleCatchUp(t *testing.T) {
	db := mocks.NewRepository(t)

	handler := New(db)

	type input struct {
		name     string
		socket   mock.FakeSocket
		ID       int
		message  []byte
		expected []message.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully catchup 4 messages on a channel

	fakeSocket := mock.FakeSocket{Id: "1"}
	ID := 1
	channel := "/root/lao1"
	messagesToCatchUp := []message.Message{
		generator.NewNothingMsg(t, "sender1", nil),
		generator.NewNothingMsg(t, "sender2", nil),
		generator.NewNothingMsg(t, "sender3", nil),
		generator.NewNothingMsg(t, "sender4", nil),
	}

	db.On("GetAllMessagesFromChannel", channel).Return(messagesToCatchUp, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator.NewCatchupQuery(t, ID, channel),
		expected: messagesToCatchUp,
		isError:  false,
	})

	// Test 2: failed to catchup because DB is disconnected

	fakeSocket = mock.FakeSocket{Id: "2"}
	ID = 2
	channel = "/root/lao2"

	db.On("GetAllMessagesFromChannel", channel).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator.NewCatchupQuery(t, ID, channel),
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
