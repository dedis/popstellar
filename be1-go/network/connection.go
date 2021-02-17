// network implements communication through websockets
package network

// file that implement a websocket. Strongly inspired from the chat example of github.com/gorilla

import (
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

//wrapper for the web socket connection
// connectionWrapper might be more apt?
type connection struct {
	// Buffered channel of outbound messages.
	// send -> sendChan/sender
	send chan []byte
	id   int
	// The hub.
	h *hub
}

func (c *connection) reader(wg *sync.WaitGroup, wsConn *websocket.Conn) {
	defer wg.Done()
	for {
		_, msg, err := wsConn.ReadMessage()
		if err != nil {
			break
		}

		// Perhaps receivedMessage can be a chan of a struct that has message and ID as fields
		// receivedMessage -> receiveChan/receiver
		c.h.idOfSender = c.id
		c.h.receivedMessage <- msg
	}
}

func (c *connection) writer(wg *sync.WaitGroup, wsConn *websocket.Conn) {
	defer wg.Done()
	for message := range c.send {
		err := wsConn.WriteMessage(websocket.TextMessage, message)
		if err != nil {
			break
		}
	}
}

var upgrader = &websocket.Upgrader{ReadBufferSize: 1024, WriteBufferSize: 1024}

type WsHandler struct {
	h *hub
}

func NewWSHandler(h *hub) WsHandler {
	return WsHandler{h: h}
}

func (wsh WsHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	upgrader.CheckOrigin = func(r *http.Request) bool { return true }
	wsConn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("error upgrading %s", err)
		return
	}

	// This does not protect against race on wsh.h.connIndex
	c := &connection{send: make(chan []byte, 256), h: wsh.h, id: wsh.h.connIndex}
	// TODO: check if we can avoid a cyclic reference between connection <--> hub
	// Perhaps we can only store a map of connectionIDs as key and senderCh as value
	c.h.addConnection(c)
	defer c.h.removeConnection(c)

	var wg sync.WaitGroup
	wg.Add(2)
	go c.writer(&wg, wsConn)
	go c.reader(&wg, wsConn)
	wg.Wait()
	err = wsConn.Close()
	if err != nil {
		fmt.Print(err)
	}
}
