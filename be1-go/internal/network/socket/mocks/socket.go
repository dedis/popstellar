package mocks

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/network/socket"
)

// FakeSocket is a fake implementation of a Socket
//
// - implements socket.Socket
type FakeSocket struct {
	socket.Socket

	ResultID    int
	Res         []mmessage.Message
	MissingMsgs map[string][]mmessage.Message
	Msg         []byte

	Err error

	// the Socket ID
	Id string

	// the rumors present in a rumor state answer
	Rumors []mrumor.Rumor
}

func NewFakeSocket(ID string) *FakeSocket {
	return &FakeSocket{Id: ID}
}

// Send implements socket.Socket
func (f *FakeSocket) Send(msg []byte) {
	f.Msg = msg
}

// SendResult implements socket.Socket
func (f *FakeSocket) SendResult(id int, res []mmessage.Message, missingMsgs map[string][]mmessage.Message) {
	f.ResultID = id
	f.Res = res
	f.MissingMsgs = missingMsgs
}

// SendError implements socket.Socket
func (f *FakeSocket) SendError(_ *int, err error) {
	f.Err = err
}

// SendPopError implements socket.Socket
func (f *FakeSocket) SendPopError(_ *int, err error) {
	f.Err = err
}

func (f *FakeSocket) SendToRandom(buf []byte) {
	f.Msg = buf
}

func (f *FakeSocket) SendRumorStateAnswer(id int, rumors []mrumor.Rumor) {
	f.Rumors = rumors
	f.ResultID = id
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
