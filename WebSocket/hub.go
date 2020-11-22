package WebSocket

import (
	"bytes"
	"fmt"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/define"
	"sync"
	"time"
	"student20_pop/db"
)

const SIG_TRESHOLD = 4

/*
TODO
faire des tests
-Subscribing
-Unsubscribing

conversion array de byte -> string /Ouriel

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

	// 1st message to send to the channel
	message []byte
	channel []byte

	// 2nd message to send to the channel
	message2 []byte
	channel2 []byte

	//Response for the sender
	idOfSender       int
	responseToSender []byte

	//msg received from the webskt
	receivedMessage chan []byte

	//Database instance
	db *bolt.DB

	logMx sync.RWMutex
	log   [][]byte

	connIndex int
}

func NewOrganizerHub() *hub {
	h := &hub{
		connectionsMx:    sync.RWMutex{},
		receivedMessage:  make(chan []byte),
		connections:      make(map[*connection]struct{}),
		db:               nil,
		connIndex:        0,
		idOfSender:       -1,
		responseToSender: nil,
		message:          nil,
		message2:         nil,
		channel2:         nil,
	}
	//publish subscribe go routine !

	go func() {
		for {
			//get msg from connection
			msg := <-h.receivedMessage
			h.message = nil
			h.responseToSender = nil
			//handle the message and generate the response
			h.HandleWholeMessage(msg, h.idOfSender)
			msgBroadcast := h.message
			msgResponse := h.responseToSender

			var subscribers []int = nil
			var err error = nil
			if bytes.Equal(h.channel, []byte("/root")) {
				subscribers, err = db.GetSubscribers(h.channel)
				if err != nil {
					log.Fatal("can't get subscribers", err)
				}
			}

			h.connectionsMx.RLock()
			for c := range h.connections {
				//send msgBroadcast to that connection if channel is main channel or is in channel subscribers
				_, found := define.Find(subscribers, c.id)

				if (bytes.Equal(h.channel, []byte("/root")) || found) && msgBroadcast != nil {
					select {
					case c.send <- msgBroadcast:
					// stop trying to send to this connection after trying for 1 second.
					// if we have to stop, it means that a reader died so remove the connection also.
					case <-time.After(1 * time.Second):
						log.Printf("shutting down connection %c", c.id)
						h.removeConnection(c)
					}
				}
			}
			for c := range h.connections {
				//send answer to client
				if h.idOfSender == c.id {
					select {

					case c.send <- msgResponse:
					// stop trying to send to this connection after trying for 1 second.
					// if we have to stop, it means that a reader died so remove the connection also.
					case <-time.After(1 * time.Second):
						log.Printf("shutting down connection %c", c.id)
						h.removeConnection(c)
					}
				}
			}

			h.connectionsMx.RUnlock()
		}
	}()
	return h
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


func (h *hub) sendResponse(conn *connection) {

}
