package WebSocket

import (
	"bytes"
	"errors"
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

//call with msg = receivedMessage
func (h *hub) HandleWholeMessage(msg []byte, userId int) error {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		return err
	}

	switch generic.Method {
	case "subscribe":
		return h.handleSubscribe(generic, userId)
	case "unsubscribe":
		return h.handleUnsubscribe(generic, userId)
	case "publish":
		return h.handlePublish(generic)
	//case "message": return h.handleMessage() // Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast. Or they are only notification, and we just want to check that it was a success
	//case "catchup": return h.handleCatchup() // TODO

	default:
		log.Fatal("JSON Method not recognized :", generic)
	}
	//TODO need to convert manually from Json ?
	return nil
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
			log.Fatal("Action on LAO not recognized :", data)
		}

	case "message":
		switch data["action"] {
		case "witness":

		default:
			log.Fatal("Action on message not recognized :", data)
		}

	case "meeting":
		switch data["action"] {
		case "create":

		case "state":

		default:
			log.Fatal("Action on meeting not recognized :", data)
		}

	default:
		log.Fatal("Object of action not recognized :", data)
	}

	return nil
}


func (h *hub) handleCreateLAO(message define.Message, canal string, generic define.Generic) error {

	if canal != "0" {
		return errors.New("tried to publish a LAO on a channel other than root")
	} 

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return err
	}

	if !define.LAOCreatedIsValid(data, message) {
		return errors.New("the LAO data wasn't valid")
	}

	lao := define.LAO{data.ID, data.Name, data.Creation, data.LastModified, data.OrganizerPKey, data.Witnesses}


	h.message <- define.CreateBroadcast(message, generic)
	h.responseToSender <- define.CreateResponse(err, generic)

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

	case []byte("subscribe"): //OURIEL
			//h.db, err = db.OpenChannelDB()
			//if err != nil {
			//	return err
			//}
			reg, err := src.JsonRegistration(message.Data)
			if err != nil {
				return err
			}

			already, err db.alreadyRegister(reg.UserID){
			if err != nil {
				return err
			}
			if(!aleady){
				err db.CreateUser(reg.UserID){
				if err != nil {
					return err
				}
			}

			err db.UpdateChannelDB(reg){
			if err != nil
				return err
			}
			//append(LAO.members, ID_Subscriber) automatically done ?


	case []byte("unsubscribe")://OURIEL
			reg, err := src.JsonRegistration(message.Data)
			if err != nil {
				return err
			}
			err db.UpdateChannelDB(reg){
			if err != nil
				return err
			}
			//remove(LAO.members, ID_Subscriber)
	case []byte("fetch"): //OURIEL
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
