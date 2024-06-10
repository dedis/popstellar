package mocks

import (
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
)

// FakeSocket is a fake implementation of a Socket
//
// - implements socket.Socket
type FakeSocket struct {
	socket.Socket

	ResultID    int
	Res         []message.Message
	MissingMsgs map[string][]message.Message
	Msg         []byte

	Err error

	// the Socket ID
	Id string
}

func NewFakeSocket(ID string) *FakeSocket {
	return &FakeSocket{Id: ID}
}

// Send implements socket.Socket
func (f *FakeSocket) Send(msg []byte) {
	f.Msg = msg
}

// SendResult implements socket.Socket
func (f *FakeSocket) SendResult(id int, res []message.Message, missingMsgs map[string][]message.Message) {
	f.ResultID = id
	f.Res = res
	f.MissingMsgs = missingMsgs
}

// SendError implements socket.Socket
func (f *FakeSocket) SendError(_ *int, err error) {
	f.Err = err
}

func (f *FakeSocket) ID() string {
	return f.Id
}

func (f *FakeSocket) GetMessage() []byte {
	return f.Msg
}

func (f *FakeSocket) Type() socket.SocketType {
	return socket.ClientSocketType
}
