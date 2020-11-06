package WebSocket

import (
	"log"
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
	//channel in wich we have to send the info
	channel chan []byte

	//msg recieved from the webskt
	recievedMessage chan []byte

	logMx sync.RWMutex
	log   [][]byte
}

func NewHub() *hub {
	h := &hub{
		connectionsMx:   sync.RWMutex{},
		message:         make(chan []byte),
		recievedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
	}
	//publish subscribe go routine !
	/*
		go func() {
			for {
				msg := <-h.message
				channel := <-h.channel
				if channel != 0 {
					send_to := getSubscribersFromChannel(channel)
				} else {
					if(isSubchannel()){
						send_to := getSubsrcriberFromParentChannel( channel) //Askip Bryan veut autre chose lol
					}
					send_to := h.connections
				}
				h.connectionsMx.RLock()
				for c := range send_to {
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
		}()*/
	go func() {
		for {

			msg := <-h.recievedMessage
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
	h.connectionsMx.Lock()
	defer h.connectionsMx.Unlock()
	h.connections[conn] = struct{}{}
}

func (h *hub) removeConnection(conn *connection) {
	h.connectionsMx.Lock()
	defer h.connectionsMx.Unlock()
	if _, ok := h.connections[conn]; ok {
		delete(h.connections, conn)
		close(conn.send)
	}
}
