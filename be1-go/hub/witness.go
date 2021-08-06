package hub

import (
	"context"
	"log"
	"student20_pop/message"
	"sync"

	"go.dedis.ch/kyber/v3"
)

// witnessHub represents a Witness and implements the Hub interface.
type witnessHub struct {
	*baseHub
}

// NewWitnessHub returns a Witness Hub.
func NewWitnessHub(public kyber.Point, wg *sync.WaitGroup) (*witnessHub, error) {
	baseHub, err := NewBaseHub(public, wg)
	return &witnessHub{
		baseHub: baseHub,
	}, err
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

	switch incomingMessage.Socket.Type() {
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

func (w *witnessHub) Type() HubType {
	return WitnessHubType
}

func (w *witnessHub) Start(ctx context.Context) {
	log.Printf("started witness...")
	w.wg.Add(1)
	defer w.wg.Done()

	for {
		select {
		case incomingMessage := <-w.messageChan:
			w.handleIncomingMessage(&incomingMessage)
		case id := <-w.closedSockets:
			w.RLock()
			for _, channel := range w.channelByID {
				// dummy Unsubscribe message because it's only used for logging...
				channel.Unsubscribe(id, message.Unsubscribe{})
			}
			w.RUnlock()
		case <-ctx.Done():
			log.Println("closing the hub...")
			return
		}
	}
}
