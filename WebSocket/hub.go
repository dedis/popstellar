package WebSocket

import (
	"fmt"
	"github.com/boltdb/bolt"
	"log"
	"sync"
	"time"
	"../db"
)

type hub struct {
	// the mutex to protect connections
	connectionsMx sync.RWMutex

	// Registered connections.
	connections map[*connection]struct{}

	// message to send to the channel
	message chan []byte
	//channel in which we have to send the info
	channel chan []byte

	//msg received from the webskt
	receivedMessage chan []byte

	//Database instance
	db *bolt.DB

	logMx sync.RWMutex
	log   [][]byte

	connIndex int
}

func NewHub() *hub {
	h := &hub{
		connectionsMx:   sync.RWMutex{},
		message:         make(chan []byte),
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		db:              nil,
		connIndex:       0,
	}
	//publish subscribe go routine !
	/*
		go func() {
			for {
				msg := <-h.message
				chann := h.channel

				var subscribers []int = nil
				if bytes.Compare(chann, []byte("0")) == 0 {
					subscribers = channel.GetSubscribers(chann)
				}

				h.connectionsMx.RLock()
				for c := range h.connections {
					//send msg to that connection if channel is main channel or is in channel subscribers
					_, found := channel.Find(subscribers, c.id)
					if bytes.Compare(h.channel, []byte("0")) == 0 || found {
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
				h.connectionsMx.RUnlock()
			}
		}()  */
	go func() {
		for {

			msg := <-h.receivedMessage
			h.connectionsMx.RLock()
			for c := range h.connections {
				select {
				case c.send <- msg:
				// stop trying to send to this connection after trying for 1 second.
				// if we have to stop, it means that a reader died so remove the connection also.
				case <-time.After(1 * time.Second):
					log.Printf("shutting down connection %s", c)
					h.removeConnection(c)
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

//call with msg = receivedMessage

func (h *hub) HandleMessage(msg []byte) error {
	//TODO
	generic, err := AnalyseGeneric(msg)
	if err != nil {
		return err
	}

	properties, err := AnalyseProperties(generic)
	if err != nil {
		return err
	}

	switch properties.Action {
	case "subscribe": Subscribe(???, ([]byte) properties.Channel)
	case "unsubscribe": Unsubscribe(???, ([]byte) properties.Channel)
	case "message":
		message, err := AnalyseMessage(properties.Message)
		if err != nil {
			return err
		}
		// TODO waiting on Pierluca/Haoqian answer relating to the method field and whether we can take object/action out of data
	case "catchup": // TODO
	case "return": // TODO
	default :
		log.Fatal("JSON not correctly formated :", msg)
	}


/*	switch message.Item {
	case []byte("LAO"):  //ROMAIN
		switch message.Action {
		case []byte("create"):
			mc, err := test.JsonLaoCreate(message.Data)
			if err != nil {
				return err
			}

			h.db, err = db.OpenChannelDB()
			if err != nil {
				return err
			}

			id, err := db.CreateLAO(mc)
			if err != nil {
				return err
			}

			h.message <- []byte("{action: , id: , ...}") //TODO waiting for protocol definition
			h.channel <- []byte("0")

	case subscribe: //OURIEL
		append(LAO.members, ID_Subscriber)

	case unsubscribe: //OURIEL
		remove(LAO.members, ID_Subscriber)

	case fetch: //OURIEL
		sendinfo(channel)
s
	case []byte("event"): //RAOUL
		switch message.Action {
		case []byte("create"):
			m, err := src.DataToMessageEventCreate(message.Data)
			if(err != nil) {
				return err
			}
			CreateChannel(m)
			//	broadcast to parent
			// useless but sticks as a reminder that we broadcast to the parent channel, the one which received the createEvent order
			h.channel <- h.channel
			h.message <- h.message

		default :
			log.Fatal("JSON not correctly formated :", msg)
		}

	default :
		log.Fatal("JSON not correctly formated :", msg)
	}
*/

	return nil

}


