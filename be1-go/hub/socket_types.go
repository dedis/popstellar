package hub

import (
	"sync"

	"github.com/gorilla/websocket"
	"github.com/rs/xid"
)

// SocketType represents different socket types
type SocketType string

const (
	// ClientSocketType denotes a client.
	ClientSocketType SocketType = "client"

	//OrganizerSocketType denotes an organizer.
	OrganizerSocketType SocketType = "organizer"

	// WitnessSocketType denotes a witness.
	WitnessSocketType SocketType = "witness"
)

func newBaseSocket(socketType SocketType, receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup) *baseSocket {
	return &baseSocket{
		id:            xid.New().String(),
		socketType:    socketType,
		receiver:      receiver,
		closedSockets: closedSockets,
		conn:          conn,
		send:          make(chan []byte, 256),
		Wait:          wg,
	}
}

// ClientSocket denotes a client socket and implements the Socket interface.
type ClientSocket struct {
	*baseSocket
}

// NewClient returns an instance of a baseSocket.
func NewClientSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup) *ClientSocket {
	return &ClientSocket{
		baseSocket: newBaseSocket(ClientSocketType, receiver, closedSockets, conn, wg),
	}
}

// OrganizerSocket denotes an organizer socket and implements the Socket interface.
type OrganizerSocket struct {
	*baseSocket
}

// NewOrganizerSocket returns a new OrganizerSocket.
func NewOrganizerSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup) *OrganizerSocket {
	return &OrganizerSocket{
		baseSocket: newBaseSocket(OrganizerSocketType, receiver, closedSockets, conn, wg),
	}
}

// WitnessSocket denotes a witness socket and implements the Socket interface.
type WitnessSocket struct {
	*baseSocket
}

// NewWitnessSocket returns a new WitnessSocket.
func NewWitnessSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup) *WitnessSocket {
	return &WitnessSocket{
		baseSocket: newBaseSocket(WitnessSocketType, receiver, closedSockets, conn, wg),
	}
}
