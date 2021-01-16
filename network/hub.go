package network

// file to implement manage opened websockets. Implements the publish-subscribe paradigm.
// inspired from the chat example of github.com/gorilla.

import (
	"bytes"
	"fmt"
	"log"
	"student20_pop/actors"
	"student20_pop/lib"
	"sync"
	"time"
)

// hub is the struct that will manage websockets connections
type hub struct {
	// the mutex to protect connections
	connectionsMx sync.RWMutex

	// Registered connections.
	connections map[*connection]struct{}

	idOfSender int
	//msg received from the sender through the websocket
	receivedMessage chan []byte

	logMx sync.RWMutex
	log   [][]byte

	actor actors.Actor

	connIndex int
}

// NewOrganizerHub returns a hub which Actor is an organizer
func NewOrganizerHub(pkey string, database string) *hub {
	return newHub("o", pkey, database)
}

// NewWitnessHub returns a hub which Actor is a Witness
func NewWitnessHub(pkey string, database string) *hub {
	return newHub("w", pkey, database)
}

// returns a hub. Function made to be used by NewOrganizerHub et NewWitnessHub
func newHub(mode string, pkey string, database string) *hub {

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
	}

	switch mode {
	case "o":
		h.actor = actors.NewOrganizer(pkey, database)
	case "w":
		h.actor = actors.NewWitness(pkey, database)
	default:
		log.Fatal("actor mode not recognized")
	}

	//publish subscribe go routine
	go func() {
		for {
			//get msg from connection
			msg := <-h.receivedMessage

			// check if messages concerns organizer
			var messageAndChannel []lib.MessageAndChannel = nil
			var response []byte = nil
			//handle the message and generate the response
			messageAndChannel, response = h.actor.HandleReceivedMessage(msg, h.idOfSender)

			h.connectionsMx.RLock()
			for _, pair := range messageAndChannel {
				h.publishOnChannel(pair.Message, pair.Channel)
			}
			h.sendResponse(response, h.idOfSender)
			h.connectionsMx.RUnlock()
		}
	}()

	return h

}

// publishOnChannel sends a message to every subscribers of a channel
func (h *hub) publishOnChannel(msg []byte, channel []byte) {
	var subscribers []int = nil
	if bytes.Equal(channel, []byte("/root")) {
		return
	}

	subscribers = h.actor.GetSubscribers(string(channel))
	if subscribers == nil {
		log.Printf("no subscribers to this channel")
		return
	}

	for c := range h.connections {
		//send msgBroadcast to that connection if channel is main channel or is in channel subscribers
		_, found := lib.FindInt(subscribers, c.id)
		// && !emptyChannel seems useless
		if (bytes.Equal(channel, []byte("/root")) || found) && msg != nil {
			select {
			case c.send <- msg:
			// stop trying to send to this connection after trying for 1 second.
			// if we have to stop, it means that a reader died so remove the connection also.
			case <-time.After(1 * time.Second):
				log.Printf("shutting down connection %c", c.id)
				h.removeConnection(c)
			}
		}
	}
}

// sendResponse sends the message msg to the connection "sender"
func (h *hub) sendResponse(msg []byte, sender int) {
	for c := range h.connections {
		//send answer to client
		if sender == c.id {
			select {
			case c.send <- msg:
			// stop trying to send to this connection after trying for 1 second.
			// if we have to stop, it means that a reader died so remove the connection also.
			case <-time.After(1 * time.Second):
				log.Printf("shutting down connection %c", c.id)
				h.removeConnection(c)
			}
		}
	}
}

func (h *hub) addConnection(conn *connection) {
	fmt.Println("new client connected")
	h.connectionsMx.Lock()
	defer h.connectionsMx.Unlock()
	h.connections[conn] = struct{}{}
	h.connIndex++ //WARNING do not decrement on remove connection. is used as ID for pub/sub
}

func (h *hub) removeConnection(conn *connection) {
	fmt.Println("client disconnected")
	h.connectionsMx.Lock()
	defer h.connectionsMx.Unlock()
	if _, ok := h.connections[conn]; ok {
		delete(h.connections, conn)
		close(conn.send)
	}
}
