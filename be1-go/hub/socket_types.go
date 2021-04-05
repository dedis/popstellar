package hub

import (
	"github.com/gorilla/websocket"
	"sync"
)

// socket types
const (
	clientSocket = "client"
	organizerSocket = "organizer"
	witnessSocket = "witness"
)

type ClientSocket struct {
	Socket
}

// NewClient returns an instance of a Socket.
func NewClientSocket (h Hub, conn *websocket.Conn) *ClientSocket {
	return &ClientSocket {
		Socket {
			socketType: clientSocket,
			hub:  h,
			conn: conn,
			send: make(chan []byte, 256),
			Wait: sync.WaitGroup{},
		},
	}
}

type OrganizerSocket struct {
	Socket
}

func NewOrganizerSocket(h Hub, conn *websocket.Conn) *OrganizerSocket {
	return &OrganizerSocket {
		Socket {
			socketType: organizerSocket,
			hub:  h,
			conn: conn,
			send: make(chan []byte, 256),
			Wait: sync.WaitGroup{},
		},
	}
}

type WitnessSocket struct {
	Socket
}

func NewWitnessSocket(h Hub, conn *websocket.Conn) *WitnessSocket {
	return &WitnessSocket {
		Socket {
			socketType: witnessSocket,
			hub:  h,
			conn: conn,
			send: make(chan []byte, 256),
			Wait: sync.WaitGroup{},
		},
	}
}