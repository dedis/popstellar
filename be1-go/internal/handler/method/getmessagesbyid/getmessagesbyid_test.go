package getmessagesbyid

import (
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/handler/method/getmessagesbyid/mocks"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mock"
	"popstellar/internal/mock/generator"
	"testing"
)

func Test_handleGetMessagesByID(t *testing.T) {
	db := mocks.NewRepository(t)

	handler := New(db)

	type input struct {
		name     string
		socket   mock.FakeSocket
		ID       int
		message  []byte
		expected map[string][]message.Message
		isError  bool
		contains string
	}

	args := make([]input, 0)

	// Test 1: successfully handled getMessagesByID and sent the result

	fakeSocket := mock.FakeSocket{Id: "1"}
	ID := 1

	expected1 := make(map[string][]message.Message)
	expected1["/root"] = []message.Message{
		generator.NewNothingMsg(t, "sender1", nil),
		generator.NewNothingMsg(t, "sender2", nil),
		generator.NewNothingMsg(t, "sender3", nil),
		generator.NewNothingMsg(t, "sender4", nil),
	}
	expected1["/root/lao1"] = []message.Message{
		generator.NewNothingMsg(t, "sender5", nil),
		generator.NewNothingMsg(t, "sender6", nil),
	}

	paramsGetMessagesByID1 := make(map[string][]string)
	for k, v := range expected1 {
		paramsGetMessagesByID1[k] = make([]string, 0)
		for _, w := range v {
			paramsGetMessagesByID1[k] = append(paramsGetMessagesByID1[k], w.MessageID)
		}
	}

	db.On("GetResultForGetMessagesByID", paramsGetMessagesByID1).Return(expected1, nil)

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator.NewGetMessagesByIDQuery(t, ID, paramsGetMessagesByID1),
		expected: expected1,
		isError:  false,
	})

	// Test 2: failed to handled getMessagesByID because DB is disconnected

	fakeSocket = mock.FakeSocket{Id: "2"}
	ID = 2

	paramsGetMessagesByID2 := make(map[string][]string)

	db.On("GetResultForGetMessagesByID", paramsGetMessagesByID2).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "Test 2",
		socket:   fakeSocket,
		ID:       ID,
		message:  generator.NewGetMessagesByIDQuery(t, ID, paramsGetMessagesByID2),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			id, err := handler.Handle(&arg.socket, arg.message)
			if arg.isError {
				require.NotNil(t, id)
				require.Error(t, err, arg.contains)
				require.Equal(t, arg.ID, *id)
			} else {
				require.NoError(t, err)
				require.NotNil(t, arg.expected)
				require.Equal(t, arg.expected, arg.socket.MissingMsgs)
			}
		})
	}
}
