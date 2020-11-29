package WebSocket

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

const SIG_TRESHOLD = 4

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

	organizer *actors.Organizer
	witness   *actors.Witness

	connIndex int
}

func NewHub() *hub {

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		organizer:       actors.NewOrganizer("", "orgDatabase.db"),
		witness:         actors.NewWitness("", "witDatabase.db"),
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
			//handle the message and generate the response, if error, print it in console
			/*check1, err := h.isForOrganizer(msg)
			if err != nil {
				fmt.Print(err)
			}
			check2, err2 := h.isForWitness(msg)
			if err2 != nil {
				fmt.Print(err2)
			}
			if check1 && check2 {
				fmt.Print("cannot be both witness and organizer")
			} else if check1 {
				message, channel, response = h.organizer.HandleWholeMessage(msg, h.idOfSender)
				fmt.Print(err)
			} else if check2 {
				//TODO
			}*/

			message, channel, response = h.organizer.HandleWholeMessage(msg, h.idOfSender)

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
	if bytes.Equal(channel, []byte("/root")) {
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

/*returns whether the Hub's organizer has the same public key as the organizer of the channel of the message*/
func (h *hub) isForOrganizer(message []byte) (bool, error) {

	gen, err := define.AnalyseGeneric(message)
	if err != nil {
		return false, err
	}
	params, err := define.AnalyseParamsFull(gen.Params)
	if err != nil {
		return false, err
	}
	//TODO extract parent channel if subChannel
	return h.organizer.IsOrganizer(params.Channel)
}

/*returns whether the Hub's Witness can witness the received message*/
func (h *hub) isForWitness(message []byte) (bool, error) {
	gen, err := define.AnalyseGeneric(message)
	if err != nil {
		return false, err
	}
	params, err := define.AnalyseParamsFull(gen.Params)
	if err != nil {
		return false, err
	}

	return h.witness.IsWitness(params.Channel)
}
