package hub

import (
	"context"
	"log"
	"student20_pop/message2/query/method"
	"student20_pop/network/socket"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
)

// witnessHub represents a Witness and implements the Hub interface.
type witnessHub struct {
	*baseHub
}

// NewWitnessHub returns a Witness Hub.
func NewWitnessHub(public kyber.Point, log zerolog.Logger) (*witnessHub, error) {
	baseHub, err := NewBaseHub(public, log)
	return &witnessHub{
		baseHub: baseHub,
	}, err
}

func (w *witnessHub) handleMessageFromOrganizer(incomingMessage *socket.IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleMessageFromClient(incomingMessage *socket.IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleMessageFromWitness(incomingMessage *socket.IncomingMessage) {
	//TODO
}

func (w *witnessHub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) {
	defer w.workers.Release(1)

	log.Printf("organizerHub::handleIncomingMessage: %s", incomingMessage.Message)

	switch incomingMessage.Socket.Type() {
	case socket.OrganizerSocketType:
		w.handleMessageFromOrganizer(incomingMessage)
		return
	case socket.ClientSocketType:
		w.handleMessageFromClient(incomingMessage)
		return
	case socket.WitnessSocketType:
		w.handleMessageFromWitness(incomingMessage)
		return
	}
}

func (w *witnessHub) Type() HubType {
	return WitnessHubType
}

func (w *witnessHub) Start() {
	log.Printf("started witness...")

	go func() {
		for {
			select {
			case incomingMessage := <-w.messageChan:
				ok := w.workers.TryAcquire(1)
				if !ok {
					log.Print("warn: worker pool full, waiting...")
					w.workers.Acquire(context.Background(), 1)
				}

				w.handleIncomingMessage(&incomingMessage)
			case id := <-w.closedSockets:
				w.RLock()
				for _, channel := range w.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				w.RUnlock()
			case <-w.stop:
				log.Println("closing the hub...")
				return
			}
		}
	}()
}

func (w *witnessHub) Stop() {
	close(w.stop)
	log.Println("Waiting for existing workers to finish...")
	w.workers.Acquire(context.Background(), numWorkers)
}
