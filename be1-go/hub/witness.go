package hub

import (
	"log"
	"sync"

	"go.dedis.ch/kyber/v3"
)
type witnessHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string] Channel

	public kyber.Point
}

// NewOrganizerHub returns a Organizer Hub.
func NewWitnessHub(public kyber.Point) Hub {
	return &witnessHub{
		messageChan: make(chan IncomingMessage),
		channelByID: make(map[string] Channel),
		public:      public,
	}
}

func (w *witnessHub) RemoveClientSocket(client *ClientSocket) {
	//TODO
	return
}

func (w *witnessHub) Recv(msg IncomingMessage) {
	//TODO
	return
}

func (w *witnessHub) handleMessageFromClient(incomingMessage *IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleMessageFromOrganizer(incomingMessage *IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleIncomingMessage(incomingMessage *IncomingMessage) {
	log.Printf("organizerHub::handleIncomingMessage: %s", incomingMessage.Message)

	switch incomingMessage.Socket.socketType {
	case clientSocket:
		w.handleMessageFromClient(incomingMessage)
		return
	case witnessSocket:
		w.handleMessageFromOrganizer(incomingMessage)
		return
	}

}

func (w *witnessHub) Start(done chan struct{}) {
	log.Printf("started witness ..")

	for {
		select {
		case incomingMessage := <-w.messageChan:
			w.handleIncomingMessage(&incomingMessage)
		case <-done:
			return
		}
	}

	return
}