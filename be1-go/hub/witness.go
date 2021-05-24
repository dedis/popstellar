package hub

import (
	"encoding/json"
	"fmt"
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

	//check if the witness needs

}

//TODO: check if it is useful
type witnessChannel struct{
	*baseChannel

	// key is a string representation of the client's public key and
	// the value is the client's signature
	clients map[string]message.PublicKeySignaturePair

	//list of client signatures
	clientSignatures []message.PublicKey
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
}

func (w *witnessHub) handleMessageFromOrganizer(incomingMessage *IncomingMessage) {
	witness := WitnessSocket{
		incomingMessage.Socket,
	}

	// unmarshal the message like we do for the client
	genericMsg := &message.GenericMessage{}
	err := json.Unmarshal(incomingMessage.Message, genericMsg)
	if err != nil {
		log.Printf("failed to unmarshal incoming message: %v", err)
	}

	query := genericMsg.Query

	if query == nil {
		return
	}
	// Does a witness even need a specific channel for different types of messages?
	channelID := query.GetChannel()
	log.Printf("channel: %s", channelID)

	id := query.GetID()

	if channelID[:6] != "/root/" {
		log.Printf("channel id must begin with /root/")
		witness.SendError(id, &message.Error{
			Code:        -2,
			Description: "channel id must begin with /root/",
		})
		return
	}

	channelID = channelID[6:]
	w.RLock()
	_, ok := w.channelByID[channelID]
	if !ok {
		log.Printf("invalid channel id: %s", channelID)
		witness.SendError(id, &message.Error{
			Code:        -2,
			Description: fmt.Sprintf("channel with id %s does not exist", channelID),
		})
		return
	}
	w.RUnlock()

	method := query.GetMethod()
	log.Printf("method: %s", method)

	var msg []message.Message

	// TODO: use constants
	switch method {
	case "subscribe":
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("No subscribe message is expected to come from other servers"),
		}
	case "unsubscribe":
		err  = &message.Error{
		Code:        -4,
		Description: fmt.Sprintf("No unsubscribe message is expected to come from other servers"),
		}
	case "publish":
		//the witness should not receive any publish messages
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("No publish message is expected to come from other servers"),
		}
	case "message":
		broadcastHelper(msg)
	case "catchup":
		//TODO: do we need to go throw the channel for this catchup?
		//msg = channel.Catchup(*query.Catchup)
		err = w.sendAllMessagesToSender(witness)
		// TODO send catchup response to client
	}

	if err != nil {
		log.Printf("failed to process query: %v", err)
		witness.SendError(id, err)
		return
	}

	result := message.Result{}

	if method == "catchup" {
		result.Catchup = msg
	} else {
		general := 0
		result.General = &general
	}

	witness.SendResult(id, result)
}
//TODO: check if we send the messages correctly to the sender of the catchup message
func (w *witnessHub)sendAllMessagesToSender(socket WitnessSocket) error{
	for _,v := range w.receivedMessages{
		buf,err := json.Marshal(v)
		if err != nil{
			return  &message.Error{
				Code:        -4,
				Description: fmt.Sprintf("Failed to Marshal an already received message"),
			}
		}
		socket.Send(buf)
	}
	return nil
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


func (w* witnessChannel)broadcastHelper(message2 []message.Message){
	for _,m:= range message2 {
		messageObj := m.Data.GetObject()
		switch messageObj {
		// This should correspond to the client's signature
		case "witness":
			//w.inbox[m.???] = m


			//TODO: check if we wil store the pk or message signature
			w.clientSignatures = append(w.clientSignatures,m.Sender)
		}
	}
}