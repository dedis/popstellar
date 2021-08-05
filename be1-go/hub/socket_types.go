package hub

import (
	"sync"

	"github.com/gorilla/websocket"
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

func newSocket(socketType SocketType, h Hub, conn *websocket.Conn, wg *sync.WaitGroup) *baseSocket {
	return &baseSocket{
		socketType: socketType,
		hub:        h,
		conn:       conn,
		send:       make(chan []byte, 256),
		Wait:       wg,
	}
}

// ClientSocket denotes a client socket and implements the Socket interface.
type ClientSocket struct {
	*baseSocket
}

// NewClient returns an instance of a baseSocket.
func NewClientSocket(h Hub, conn *websocket.Conn, wg *sync.WaitGroup) *ClientSocket {
	return &ClientSocket{
		newSocket(ClientSocketType, h, conn, wg),
	}
}

// OrganizerSocket denotes an organizer socket and implements the Socket interface.
type OrganizerSocket struct {
	*baseSocket
}

// NewOrganizerSocket returns a new OrganizerSocket.
func NewOrganizerSocket(h Hub, conn *websocket.Conn, wg *sync.WaitGroup) *OrganizerSocket {
	return &OrganizerSocket{
		newSocket(OrganizerSocketType, h, conn, wg),
	}
}

// WitnessSocket denotes a witness socket and implements the Socket interface.
type WitnessSocket struct {
	*baseSocket
}

// NewWitnessSocket returns a new WitnessSocket.
func NewWitnessSocket(h Hub, conn *websocket.Conn, wg *sync.WaitGroup) *WitnessSocket {
	return &WitnessSocket{
		newSocket(WitnessSocketType, h, conn, wg),
	}
}
