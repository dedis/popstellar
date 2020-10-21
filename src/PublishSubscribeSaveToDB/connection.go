package main

import (
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

		// store msg in DB
		database.Update(func(tx *bolt.Tx) error {
			b, err1 := tx.CreateBucketIfNotExists([]byte("MyBucket"))
			if err1 != nil {
				log.Fatal(err1)
				return err1
			}
			err2 := b.Put([]byte(strconv.Itoa(i)), message)
			if err2 != nil {
				log.Fatal(err2)
				return err2
			}
			i++
			return nil

		})
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

type wsHandler struct {
	h *hub

	// the database
	database *bolt.DB
}

func (wsh wsHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
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

func (wsh wsHandler) HandleMessage(msg string) {
	//"unpack message"

	//local actions depending on message

	//send response to client

	//return code
}
