package hub

import (
	"log"
	"student20_pop/message"
	"sync"

	"go.dedis.ch/kyber/v3"
)
type witnessHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string] Channel

	public kyber.Point

	//TODO:check this is only a draft
	receivedMessages map[string][]message.Message
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
/**
	This function should capture all the messages that are sent while an election
	is happening
 */
func (w *witnessHub) Recv(msg IncomingMessage) {
	//TODO
	return
}
/**
	This is where all the witness logic should happen for all the messages received from client
	-the witness should stash the messages
	-the witness should sign the messages
	-the witness should deliver the signed messages to attendee/organizer
 */
func (w *witnessHub) handleMessageFromClient(incomingMessage *IncomingMessage) {
	//TODO
	//check exactly how we will store a message once we receive it
	//TODO: find the sender so it can represent the key in the stored messages
	w.receivedMessages[incomingMessage.Socket.send] = incomingMessage.Message
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