// Package socket contains an implementation of the Socket interface which
// is responsible for low level communication over websockets, i.e. sending
// and receiving marshaled messages over the wire.
//
// The Socket interface has multiple concrete implementations - one for servers
// and one for clients.
package socket

import (
	"popstellar/message/query/method/message"
	"time"
)

const (
	// maxMessageSize denotes a maximum possible message size in bytes
	maxMessageSize = 256 * 1024 // 256K

	// writeWait denotes the timeout for writing.
	writeWait = 10 * time.Second

	// pongWait is the timeout for reading a pong.
	pongWait = 60 * time.Second

	// pingPeriod is the interval to send ping messages in.
	pingPeriod = (pongWait * 9) / 10
)

// Socket is an interface which allows reading/writing messages to
// another client
type Socket interface {
	// ID denotes a unique ID of the socket. This allows us to store
	// sockets in maps.
	ID() string

	// Type denotes the type of socket.
	Type() SocketType

	// Address denotes the address.
	Address() string

	// ReadPump is a lower level method for reading messages from the socket.
	ReadPump()

	// WritePump is a lower level method for writing messages to the socket.
	WritePump()

	// Send is used to send a message to the client.
	Send(msg []byte)

	// SendError is used to send an error to the client.  Please refer to
	// the Protocol Specification document for information on the error
	// codes. id is a pointer type because an error might be for a
	// message which does not have an ID.
	SendError(id *int, err error)

	// SendResult is used to send a result message to the client. Res can be
	// nil, empty, or filled if the result is a slice of messages.
	// MissingMessagesByChannel can be nil or filled if the result is a map
	// associating a channel to a slice of messages. In case both are nil
	// it sends the "0" return value. You can either send res or missingMessagesByChannel, not both.
	SendResult(id int, res []message.Message, missingMessagesByChannel map[string][]message.Message)
}

// IncomingMessage wraps the raw message from the websocket connection and pairs
// it with a `Socket` instance.
type IncomingMessage struct {
	// Socket denotes where the message is originating from
	// and allows us to communicate with the other party.
	Socket Socket

	// Message is the marshaled message
	Message []byte
}
