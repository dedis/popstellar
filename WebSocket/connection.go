package WebSocket

import (
	"student20_pop/db"
	src "student20_pop/define"

	"log"
	"net/http"
	"strconv"
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
	i := 0
	for {
		_, message, err := wsConn.ReadMessage()
		if err != nil {
			break
		}
		c.h.broadcast <- message
		key := []byte(strconv.Itoa(i))
		bkt := []byte("MyBucket")
		// store msg in DB
		err = db.Write(key, message, bkt, database)
		if err != nil {
			log.Fatal(err)
		}
		i++
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

func (wsh WsHandler) HandleMessage(msg []byte) error {

	message, err := src.AnalyseMsg(msg)
	if err != nil {
		return err
	}

	switch message.Item {
	case []byte("lao"):
		switch message.Action {
		case []byte("create"):
			laomsg, err := src.JsonLaoCreate(message.Data)
			if err != nil {
				return err
			}
			id, err := db.CreateLAO(laomsg)

			// TODO send back a message : success lao creation + id
		}

	}

	return err

	//"unpack message"
	//local actions depending on message
	/*
		type = analyse(msg)
		switch type
		case LAO :
			action = getaction(msg)
			switch action
				case get : lao = getFromID(msg.getID)
							client.send(lao.toJSON)

	*/
	// LAO --> new LAO(name : ..., id : ..., ...)
	// switch case: create --> CreateLAO(...)
	// getFromID(....)
	//send response to client
	//return code
}
