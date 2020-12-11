/* file to implement manage opened websockets. Implements the publish-subscribe paradigm.
inspired from the chat example of github.com/gorilla */

package websocket

import (
	"bytes"
	"fmt"
	"log"
	"student20_pop/actors"
	"student20_pop/db"
	"student20_pop/define"
	"sync"
	"time"
)

/*
TODO
faire des tests
-Subscribing
-Unsubscribing

Propagating a message on a channel
Catching up on past messages on a channel /RAOUl

Publish a message on a channel:
Update LAO properties ->
LAO state broadcast ->
Witness a message ->

Creating a 'event' !
#check from witness
#verify if witnessed
-meeting/ Ouriel
-roll call/ ouriel
-discussion(?)
-poll/ouriel
-cast vote
-register attendance
Meeting state broadcast
*/
type hub struct {
	// the mutex to protect connections
	connectionsMx sync.RWMutex

	// Registered connections.
	connections map[*connection]struct{}

	//Response for the sender
	idOfSender int
	//msg received from the sender through the websocket
	receivedMessage chan []byte

	logMx sync.RWMutex
	log   [][]byte

	actor actors.Actor

	connIndex int
}

func NewOrganizerHub(pkey string) *hub {
	return newHub("o", pkey)
}

func NewWitnessHub(pkey string) *hub {
	return newHub("w", pkey)
}

func newHub(mode string, pkey string) *hub {

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
	}

	if mode == "o" {
		h.actor = actors.NewOrganizer(pkey, "orgdatabase.db")
	} else if mode == "w" {
		h.actor = actors.NewWitness(pkey, "witdatabase.db")
	} else {
		log.Fatal("actor mode not recognized")
	}
	//publish subscribe go routine !

	go func() {
		for {
			//get msg from connection
			msg := <-h.receivedMessage

			// check if messages concerns organizer
			var message []byte = nil
			var channel []byte = nil
			var response []byte = nil
			//handle the message and generate the response
			message, channel, response = h.actor.HandleWholeMessage(msg, h.idOfSender)

			h.connectionsMx.RLock()
			h.publishOnChannel(message, channel)
			h.sendResponse(response, h.idOfSender)
			h.connectionsMx.RUnlock()
		}
	}()
	return h
}

/* sends the message msg to every subscribers of the channel channel */
func (h *hub) publishOnChannel(msg []byte, channel []byte) {

	var subscribers []int = nil
	var err error = nil
	if !bytes.Equal(channel, []byte("/root")) {
		subscribers, err = db.GetSubscribers(channel)
		if err != nil {
			log.Fatal("can't get subscribers", err)
		}
	}

	for c := range h.connections {
		//send msgBroadcast to that connection if channel is main channel or is in channel subscribers
		_, found := define.Find(subscribers, c.id)

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

/*sends the message msg to the connection sender*/
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
	// QUESTION what if connection is already in the map ?
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
