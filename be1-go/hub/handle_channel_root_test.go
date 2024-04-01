package hub

import (
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"io"
	"popstellar/hub/mocks"
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"testing"
)

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID    int
	res         []message.Message
	missingMsgs map[string][]message.Message
	msg         []byte

	err error

	// the socket ID
	id string
}

// Send implements socket.Socket
func (f *fakeSocket) Send(msg []byte) {
	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message, missingMsgs map[string][]message.Message) {
	f.resultID = id
	f.res = res
	f.missingMsgs = missingMsgs
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	f.err = err
}

func (f *fakeSocket) ID() string {
	return f.id
}

func (f *fakeSocket) Type() socket.SocketType {
	return socket.ClientSocketType
}

func newHandlerParameters(db Repository) handlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	peers := state.NewPeers()
	queries := state.NewQueries(nolog)

	return handlerParameters{
		log:                 nolog,
		socket:              fakeSocket{id: "fakeID"},
		schemaValidator:     *schemaValidator,
		db:                  db,
		subs:                make(subscribers),
		peers:               &peers,
		queries:             &queries,
		ownerPubKey:         nil,
		clientServerAddress: "clientServerAddress",
		serverServerAddress: "serverServerAddress",
	}

}

func Test_MockExample(t *testing.T) {
	repo := mocks.NewRepository(t)
	repo.On("GetMessageByID", "messageID1").Return(message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}, nil)
	msg, err := repo.GetMessageByID("messageID1")
	if err != nil {
		return
	}
	log.Info().Msg(msg.MessageID)
}

func Test_handleChannelRoot(t *testing.T) {
	mockRepository := mocks.NewRepository(t)

	type args struct {
		params  handlerParameters
		channel string
		msg     message.Message
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "Test1",
			args: args{
				params:  newHandlerParameters(mockRepository),
				channel: "channel",
				msg: message.Message{
					Data:              "data",
					Sender:            "sender",
					Signature:         "sig",
					MessageID:         "ID",
					WitnessSignatures: nil,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handleChannelRoot(tt.args.params, tt.args.channel, tt.args.msg)
		})
	}
}
