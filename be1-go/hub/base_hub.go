package hub

import (
	"log"
	"sync"

	"student20_pop/message"

	"go.dedis.ch/kyber/v3"
)

type baseHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	public kyber.Point
}

// NewBaseHub returns a Base Hub.
func NewBaseHub(public kyber.Point) *baseHub {
	return &baseHub{
		messageChan: make(chan IncomingMessage),
		channelByID: make(map[string]Channel),
		public:      public,
	}
}

// RemoveClient removes the client from this hub.
func (h *baseHub) RemoveClientSocket(client *ClientSocket) {
	h.RLock()
	defer h.RUnlock()

	for _, channel := range h.channelByID {
		channel.Unsubscribe(client, message.Unsubscribe{})
	}
}

// Recv accepts a message and enques it for processing in the hub.
func (h *baseHub) Recv(msg IncomingMessage) {
	log.Printf("Hub::Recv")
	h.messageChan <- msg
}

func start(h Hub, done chan struct{}, messageChan chan IncomingMessage) {
	for {
		select {
		case incomingMessage := <-messageChan:
			h.handleIncomingMessage(&incomingMessage)
		case <-done:
			return
		}
	}
}
