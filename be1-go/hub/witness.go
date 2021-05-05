package hub

import (
	"log"

	"go.dedis.ch/kyber/v3"
)

type witnessHub struct {
	*baseHub
}

// NewWitnessHub returns a Witness Hub.
func NewWitnessHub(public kyber.Point) Hub {
	return &witnessHub{
		NewBaseHub(public),
	}
}

func (w *witnessHub) handleMessageFromOrganizer(incomingMessage *IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleMessageFromClient(incomingMessage *IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleMessageFromWitness(incomingMessage *IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleIncomingMessage(incomingMessage *IncomingMessage) {
	log.Printf("organizerHub::handleIncomingMessage: %s", incomingMessage.Message)

	switch incomingMessage.Socket.socketType {
	case OrganizerSocketType:
		w.handleMessageFromOrganizer(incomingMessage)
		return
	case ClientSocketType:
		w.handleMessageFromClient(incomingMessage)
		return
	case WitnessSocketType:
		w.handleMessageFromWitness(incomingMessage)
		return
	}

}

func (w *witnessHub) Start(done chan struct{}) {
	log.Printf("started witness ..")
	start(w, done, w.messageChan)
}
