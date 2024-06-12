package hheartbeat

import (
	"encoding/json"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	mocks2 "popstellar/internal/handler/method/heartbeat/hheartbeat/mocks"
	"popstellar/internal/network/socket/mocks"
	"popstellar/internal/test/generator"
	"testing"
)

func Test_handleHeartbeat(t *testing.T) {
	queries := mocks2.NewQueries(t)
	db := mocks2.NewRepository(t)

	handler := New(queries, db)

	type input struct {
		name     string
		socket   *mocks.FakeSocket
		message  []byte
		expected map[string][]string
		isError  bool
		contains string
	}

	msgIDs := []string{"msg0", "msg1", "msg2", "msg3", "msg4", "msg5", "msg6"}

	args := make([]input, 0)

	// Test 1: successfully handled heartbeat with some messages to catching up

	fakeSocket := mocks.NewFakeSocket("1")

	heartbeatMsgIDs1 := make(map[string][]string)
	heartbeatMsgIDs1["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	heartbeatMsgIDs1["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
	}
	heartbeatMsgIDs1["root/lao2"] = []string{
		msgIDs[5],
		msgIDs[6],
	}

	expected1 := make(map[string][]string)
	expected1["/root"] = []string{
		msgIDs[1],
		msgIDs[2],
	}
	expected1["root/lao1"] = []string{
		msgIDs[4],
	}

	db.On("GetParamsForGetMessageByID", heartbeatMsgIDs1).Return(expected1, nil)
	queries.On("GetNextID").Return(1)
	queries.On("AddQuery", 1, mock.AnythingOfType("mgetmessagesbyid.GetMessagesById"))

	args = append(args, input{
		name:     "Test 1",
		socket:   fakeSocket,
		message:  generator.NewHeartbeatQuery(t, heartbeatMsgIDs1),
		expected: expected1,
		isError:  false,
	})

	// Test 2: successfully handled heartbeat with nothing to catching up

	fakeSocket = mocks.NewFakeSocket("2")

	heartbeatMsgIDs2 := make(map[string][]string)
	heartbeatMsgIDs2["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}

	db.On("GetParamsForGetMessageByID", heartbeatMsgIDs2).Return(nil, nil)

	args = append(args, input{
		name:    "Test 2",
		socket:  fakeSocket,
		message: generator.NewHeartbeatQuery(t, heartbeatMsgIDs2),
		isError: false,
	})

	// Test 3: failed to handled heartbeat because DB is disconnected

	fakeSocket = mocks.NewFakeSocket("3")

	heartbeatMsgIDs3 := make(map[string][]string)
	heartbeatMsgIDs3["/root"] = []string{
		msgIDs[0],
		msgIDs[1],
		msgIDs[2],
	}
	heartbeatMsgIDs3["root/lao1"] = []string{
		msgIDs[3],
		msgIDs[4],
	}

	db.On("GetParamsForGetMessageByID", heartbeatMsgIDs3).
		Return(nil, xerrors.Errorf("DB is disconnected"))

	args = append(args, input{
		name:     "failed to popquery DB",
		socket:   fakeSocket,
		message:  generator.NewHeartbeatQuery(t, heartbeatMsgIDs3),
		isError:  true,
		contains: "DB is disconnected",
	})

	// run all tests

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			_, err := handler.Handle(arg.socket, arg.message)
			if arg.isError {
				require.Error(t, err)
			} else if arg.expected != nil {
				require.NoError(t, err)
				require.NotNil(t, arg.socket.Msg)

				var getMessageByID mgetmessagesbyid.GetMessagesById
				err := json.Unmarshal(arg.socket.Msg, &getMessageByID)
				require.NoError(t, err)
				require.Equal(t, arg.expected, getMessageByID.Params)
			} else {
				require.NoError(t, err)
				require.Nil(t, arg.socket.Msg)
			}
		})
	}
}
