package WebSocket

import (
	"bytes"
	"fmt"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/channel"
	"student20_pop/define"
	"sync"
	"time"
)

type hub struct {
	// the mutex to protect connections
	connectionsMx sync.RWMutex

	// Registered connections.
	connections map[*connection]struct{}

	// message to send to the channel
	message chan []byte
	//channel in which we have to send the info
	channel []byte

	//msg received from the webskt
	receivedMessage chan []byte

	//Database instance
	db *bolt.DB

	logMx sync.RWMutex
	log   [][]byte

	connIndex int

	//Response for the sender
	idOfSender       int
	responseToSender chan []byte
}

func NewHub() *hub {
	h := &hub{
		connectionsMx:    sync.RWMutex{},
		message:          make(chan []byte),
		receivedMessage:  make(chan []byte),
		connections:      make(map[*connection]struct{}),
		db:               nil,
		connIndex:        0,
		idOfSender:       -1,
		responseToSender: make(chan []byte),
	}
	//publish subscribe go routine !

	go func() {
		for {
			msg := <- h.receivedMessage
			h.HandleWholeMessage(msg, h.idOfSender)
			msg = <- h.message

			chann := h.channel

			var subscribers []int = nil
			var err error = nil
			if bytes.Compare(chann, []byte("0")) != 0 {
				subscribers, err = channel.GetSubscribers(chann)
				if err != nil {
					log.Fatal(err)
				}
			}



			h.connectionsMx.RLock()
			for c := range h.connections {
				//send msg to that connection if channel is main channel or is in channel subscribers
				_, found := define.Find(subscribers, c.id)
				if bytes.Compare(h.channel, []byte("0")) == 0 || found {
					select {
					case c.send <- msg:
					// stop trying to send to this connection after trying for 1 second.
					// if we have to stop, it means that a reader died so remove the connection also.
					case <-time.After(1 * time.Second):
						log.Printf("shutting down connection %c", c.id)
						h.removeConnection(c)
					}
					//TODO where to put these 3 lines?

					// TODO resp := []byte(define.ResponseToSenderInJson(errors.As(err)))
					h.responseToSender <- []byte("") // TODO resp
				}
			}
			h.connectionsMx.RUnlock()
		}
	}() /*
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
		}()/*
		go func() {
			for {
				resp := <-h.responseToSender
				h.connectionsMx.RLock()
				for c := range h.connections {
					//send msg to that connection if channel is the same channel as the sender
					if bytes.Compare(h.responseToSender, []byte("0")) == 0 {
						c.send <- resp
					}
				}
				h.connectionsMx.RUnlock()
			}
		}()*/
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

// Test json input to create LAO:
// TODO careful with base64
// TODO careful with comma after witnesses[] and witnesses_signatures[]
/*
{"jsonrpc": "2.0", "method": "publish", "params": { "channel": "0", "message": { "data": { "object": "lao", "action": "create", "id": "0x123a", "name": "My LAO", "creation": 123, "last_modified": 123, "organizer": "0x123a", "witnesses": [], }, "sender": "0x123a", "signature": "0x123a", "message_id": "0x123a", "witness_signatures": [],} }, "id": 3}

*/

// Param msg = receivedMessage
// output by setting h.responseToSender and h.broadcast
func (h *hub) HandleWholeMessage(msg []byte, userId int) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		h.responseToSender <- define.CreateResponse(err, generic)
		return
	}


	switch generic.Method {
	case "subscribe":
		err = h.handleSubscribe(generic, userId)
	case "unsubscribe":
		err = h.handleUnsubscribe(generic, userId)
	case "publish":
		err = h.handlePublish(generic)
	//case "message": return h.handleMessage() // Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast. Or they are only notification, and we just want to check that it was a success
	//case "catchup": return h.handleCatchup() // TODO

	default:
		err = define.ErrRequestDataInvalid
	}

	h.responseToSender <- define.CreateResponse(err, generic)
}

func (h *hub) handleSubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return err
	}
	return channel.Subscribe(userId, []byte(params.Channel))
}

func (h *hub) handleUnsubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return err
	}
	return channel.Unsubscribe(userId, []byte(params.Channel))
}

func (h *hub) handlePublish(generic define.Generic) error {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		return err
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		return err
	}

	data, err := define.AnalyseData(message.Data)
	if err != nil {
		return err
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return h.handleCreateLAO(message, params.Channel, generic)
		case "update_properties":

		case "state":

		default:
			return define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":

		default:
			return define.ErrInvalidAction
		}

	case "meeting":
		switch data["action"] {
		case "create":

		case "state":

		default:
			return define.ErrInvalidAction
		}

	default:
		return define.ErrRequestDataInvalid
	}

	return nil
}


func (h *hub) handleCreateLAO(message define.Message, canal string, generic define.Generic) error {

	if canal != "0" {
		return define.ErrInvalidResource
	} 

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return err
	}

	err = define.LAOCreatedIsValid(data, message)
	if err != nil {
		return err
	}

	lao := define.LAO{data.ID, data.Name, data.Creation, data.LastModified, data.OrganizerPKey, data.Witnesses}


	h.message <- define.CreateBroadcast(message, generic)

	return channel.CreateLAO(lao)
}


func (h *hub) handleMessage(msg []byte, userId int) error {

	return nil
}

// TODO
func (h *hub) handleCatchup() error {

	return nil
}

func (h *hub) sendResponse(conn *connection) {

}
