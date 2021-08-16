package hub

import (
	"context"
	"log"
	"student20_pop/message"
	"student20_pop/network/socket"

	"go.dedis.ch/kyber/v3"
)

// witnessHub represents a Witness and implements the Hub interface.
type witnessHub struct {
	*baseHub
}

// NewWitnessHub returns a Witness Hub.
func NewWitnessHub(public kyber.Point) (*witnessHub, error) {
	baseHub, err := NewBaseHub(public)
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
	defer w.workersWg.Done()
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
				w.workers.Acquire(context.Background(), 1)
				w.workersWg.Add(1)
				w.handleIncomingMessage(&incomingMessage)
			case id := <-w.closedSockets:
				w.RLock()
				for _, channel := range w.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, message.Unsubscribe{})
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
	w.workersWg.Wait()
}
