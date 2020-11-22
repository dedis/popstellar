package WebSocket

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/channel"
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
				subscribers, err = channel.GetSubscribers(h.channel)
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

// Test json input to create LAO:
//  careful with base64 needed to remove
//  careful with comma after witnesses[] and witnesses_signatures[] needed to remove

// Param msg = receivedMessage
// output by setting h.responseToSender and h.broadcast
func (h *hub) HandleWholeMessage(msg []byte, userId int) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		err = define.ErrRequestDataInvalid
		h.responseToSender = define.CreateResponse(err, nil, generic)
		return
	}

	var history []byte = nil

	switch generic.Method {
	case "subscribe":
		err = h.handleSubscribe(generic, userId)
	case "unsubscribe":
		err = h.handleUnsubscribe(generic, userId)
	case "publish":
		err = h.handlePublish(generic)
	//case "message": err = h.handleMessage() // Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast. Or they are only notification, and we just want to check that it was a success
	case "catchup":
		history, err = h.handleCatchup(generic)
	default:
		err = define.ErrRequestDataInvalid
	}

	h.responseToSender = define.CreateResponse(err, history, generic)
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
			msg, _ := json.Marshal(generic)
			return h.handleUpdateProperties(params.Channel, msg)
		case "state":

		default:
			return define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			msg, err1 := json.Marshal(generic)
			if err1 != nil {
				return err1
			}
			err := h.handleWitnessMessage(params.Channel, msg)
			if err != nil {
				return err
			}
			if len(message.WitnessSignatures) == SIG_TRESHOLD-1 {
				//TODO: update state and send state broadcast
			}
			return err
		default:
			return define.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return h.handleCreateRollCall(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return h.handleCreateMeeting(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return h.handleCreatePoll(message, params.Channel, generic)
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

	err = define.LAOCreatedIsValid(data, message)
	if err != nil {
		return err
	}

	canalLAO := canal + data.ID

	err = channel.StoreMessage(message, canalLAO)
	if err != nil {
		return err
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		LastModified:  data.LastModified,
		OrganizerPKey: data.OrganizerPKey,
		Witnesses:     data.Witnesses,
	}
	err = channel.CreateObject(lao)
	if err != nil {
		return err
	}

	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) handleCreateRollCall(message define.Message, canal string, generic define.Generic) error {
	if canal != "/root" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateRollCall(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	//TODO do we move to multiple type ? cf datadef
	event := define.Event{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = channel.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) handleCreateMeeting(message define.Message, canal string, generic define.Generic) error {

	if canal != "0" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateMeeting(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	event := define.Event{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = channel.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) finalizeHandling(message define.Message, canal string, generic define.Generic) error {
	h.message = define.CreateBroadcastMessage(message, generic)
	h.channel = []byte(canal)
	return nil
}

func (h *hub) handleCreatePoll(message define.Message, canal string, generic define.Generic) error {

	if canal != "0" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreatePoll(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	event := define.Event{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = channel.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}
func (h *hub) handleMessage(msg []byte, userId int) error {

	return nil
}

// This is organizer implementation. If Witness, should return a witness msg on object
func (h *hub) handleUpdateProperties(canal string, msg []byte) error {
	h.message = msg
	h.channel = []byte(canal)
	return channel.WriteMessage(msg, true)
}

func (h *hub) handleWitnessMessage(canal string, msg []byte) error {
	//TODO verify signature correctness
	// decrypt msg and compare with hash of "local" data

	//add signature to already stored message:
	//extract messageID and Witness signature from received message
	r_g, err := define.AnalyseGeneric(msg)
	r_p, err := define.AnalyseParamsFull(r_g.Params)
	r_d, err := define.AnalyseMessage(r_p.Message)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	//retrieve message to sign from database
	toSign, err := channel.GetMessage([]byte(canal), []byte(r_d.MessageID))
	//extract signature list
	g := define.Generic{}
	p := define.ParamsFull{}
	m := define.Message{}
	err = json.Unmarshal(toSign, &g)
	err = json.Unmarshal(g.Params, &p)
	err = json.Unmarshal(p.Message, &m)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	//if message was already signed by this witness, returns an error
	_, found := define.FindStr(m.WitnessSignatures, r_d.Signature)
	if found {
		return define.ErrResourceAlreadyExists
	}

	m.WitnessSignatures = append(m.WitnessSignatures, r_d.Signature)
	//build the string back
	str, err := json.Marshal(m)
	p.Message = str
	str, err = json.Marshal(p)
	g.Params = str
	str, err = json.Marshal(g)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	//store received message in DB and update "LAOUpdateProperties" message in DB
	err = channel.WriteMessage(str, false)
	err = channel.WriteMessage(msg, true)
	if err != nil {
		return define.ErrDBFault
	}

	//broadcast received message
	h.message = msg
	h.channel = []byte(canal)
	return nil
}

// TODO
func (h *hub) handleCatchup(generic define.Generic) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return nil, define.ErrRequestDataInvalid
	}
	history, err := channel.GetData([]byte(params.Channel))

	return history, err
}

func (h *hub) sendResponse(conn *connection) {

}
