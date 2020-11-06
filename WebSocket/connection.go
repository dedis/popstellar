package WebSocket

import (
	"log"
	"net/http"
	"sync"

	"github.com/boltdb/bolt"
	"github.com/gorilla/websocket"
)

//wrapper for the web socket connection
type connection struct {
	// Buffered channel of outbound messages.
	send chan []byte

	// The hub.
	h *hub
}

func (c *connection) reader(wg *sync.WaitGroup, wsConn *websocket.Conn, database *bolt.DB) {
	defer wg.Done()

	for {
		_, message, err := wsConn.ReadMessage()
		if err != nil {
			break
		}
		// mets le message dans les trucs à broadcast du channel


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

	// the database
	database *bolt.DB
}

func NewWSHandler(h *hub, db *bolt.DB) WsHandler {
	return WsHandler{h: h, database: db}
}

func (wsh WsHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	wsConn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("error upgrading %s", err)
		return
	}

	c := &connection{send: make(chan []byte, 256), h: wsh.h}
	c.h.addConnection(c)
	defer c.h.removeConnection(c)
	var wg sync.WaitGroup
	wg.Add(2)
	go c.writer(&wg, wsConn)
	go c.reader(&wg, wsConn, wsh.database)
	wg.Wait()
	wsConn.Close()
}

//-->  HUB
func (wsh WsHandler) HandleMessage(msg []byte) error {
	//TODO

	switch (msg) {
	case newLAO:
		c.h.broadcast <- "NEW LAO CREATED, WAAAW"

	case subscribe:
		append(LAO.members, ID_Subscriber)

	case unsubscribe:
		remove(LAO.members, ID_Subscriber)

	case fetch:
		sendinfo(channel)

	case newEvent(Channel):
		createEvent In channel
		broadcast to channel

	case default :
		log.Fatal("JSON not correctly formated :", msg)

	}
	/*
		- publish to channel :
		- Subscribe to channel :
		- create :
	*/
	return nil

}
