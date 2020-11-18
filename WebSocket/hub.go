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

	responseToSenderNotChan []byte
	messageToBroadcast      []byte
}

func NewHub() *hub {
	h := &hub{
		connectionsMx:           sync.RWMutex{},
		message:                 make(chan []byte),
		receivedMessage:         make(chan []byte),
		connections:             make(map[*connection]struct{}),
		db:                      nil,
		connIndex:               0,
		idOfSender:              -1,
		responseToSender:        make(chan []byte),
		responseToSenderNotChan: nil,
		messageToBroadcast:      nil,
	}
	//publish subscribe go routine !

	go func() {
		for {
			//get msg from connection
			msg := <-h.receivedMessage
			h.messageToBroadcast = nil
			h.responseToSenderNotChan = nil
			//handle the message and generate the response
			h.HandleWholeMessage(msg, h.idOfSender)
			msgBroadcast := h.messageToBroadcast
			msgResponse := h.responseToSenderNotChan

			var subscribers []int = nil
			var err error = nil
			if bytes.Compare(h.channel, []byte("/root")) != 0 {
				subscribers, err = channel.GetSubscribers(h.channel)
				if err != nil {
					log.Fatal("can't get subscribers", err)
				}
			}

			h.connectionsMx.RLock()
			for c := range h.connections {
				//send msgBroadcast to that connection if channel is main channel or is in channel subscribers
				_, found := define.Find(subscribers, c.id)

				if (bytes.Compare(h.channel, []byte("/root")) == 0 || found) && msgBroadcast != nil {
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

// Test json input to create LAO:
//  careful with base64 needed to remove
//  careful with comma after witnesses[] and witnesses_signatures[] needed to remove

// Param msg = receivedMessage
// output by setting h.responseToSender and h.broadcast
func (h *hub) HandleWholeMessage(msg []byte, userId int) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		err = define.ErrRequestDataInvalid
		h.responseToSenderNotChan = define.CreateResponse(err, generic)
		return
	}

	var history []string = nil

	switch generic.Method {
	case "subscribe":
		err = h.handleSubscribe(generic, userId)
	case "unsubscribe":
		err = h.handleUnsubscribe(generic, userId)
	case "publish":
		err = h.handlePublish(generic)
	//case "message": err = h.handleMessage() // Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast. Or they are only notification, and we just want to check that it was a success
	case "catchup": 
		(history, err) = h.handleCatchup(generic)
	default:
		err = define.ErrRequestDataInvalid
	}

	h.responseToSenderNotChan = define.CreateResponse(err, history, generic)
}

func (h *hub) handleSubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return channel.Subscribe(userId, []byte(params.Channel))
}

func (h *hub) handleUnsubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return channel.Unsubscribe(userId, []byte(params.Channel))
}

func (h *hub) handlePublish(generic define.Generic) error {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	data, err := define.AnalyseData(message.Data)
	if err != nil {
		return define.ErrRequestDataInvalid
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

	if canal != "/root" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// TODO
	/*err = define.LAOCreatedIsValid(data, message)
	if err != nil {
		return define.ErrAccessDenied
	}*/

	lao := define.LAO{ID: data.ID, Name: data.Name, Creation: data.Creation, LastModified: data.LastModified, OrganizerPKey: data.OrganizerPKey, Witnesses: data.Witnesses}

	err = channel.CreateLAO(lao)
	if err != nil {
		return err
	}
	h.messageToBroadcast = define.CreateBroadcastMessage(message, generic)
	h.channel = []byte(canal)
	return nil
}

func (h *hub) handleMessage(msg []byte, userId int) error {

	return nil
}

// TODO
func (h *hub) handleCatchup(generic define.Generic) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return (nil, define.ErrRequestDataInvalid)
	}
	(history, err) := channel.GetData([]byte (params.Channel))

	return (history, err)
}

func (h *hub) sendResponse(conn *connection) {

}
