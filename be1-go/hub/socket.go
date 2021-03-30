package hub

import (
	"encoding/json"
	"log"
	"student20_pop/message"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"golang.org/x/xerrors"
)

const (
	// maxMessageSize denotes a meximum possible message size in bytes
	maxMessageSize = 256 * 1024

	writeWait = 10 * time.Second

	pongWait = 60 * time.Second

	pingPeriod = (pongWait * 9) / 10
)


// Socket represents a socket connected to the server.
type Socket struct {
	socketType string

	hub Hub

	conn *websocket.Conn

	send chan []byte

	Wait sync.WaitGroup
}

// NewSocket returns an instance of a Socket.
func NewSocket(h Hub, conn *websocket.Conn) *Socket {
	return &Socket{
		hub:  h,
		conn: conn,
		send: make(chan []byte, 256),
		Wait: sync.WaitGroup{},
	}
}

// ReadPump starts the reader loop for the socket.
func (s *Socket) ReadPump() {
	defer func() {
		s.conn.Close()
		s.Wait.Done()
	}()

	s.Wait.Add(1)

	log.Printf("listening for messages from socket")

	s.conn.SetReadLimit(maxMessageSize)
	s.conn.SetReadDeadline(time.Now().Add(pongWait))
	s.conn.SetPongHandler(func(string) error { s.conn.SetReadDeadline(time.Now().Add(pongWait)); return nil })
	for {
		_, message, err := s.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("connection dropped unexpectedly: %v", err)
			}
			break
		}

		// TODO: validate message using JSON schema and unmarshal it to generic
		// message at this stage

		msg := IncomingMessage{
			Socket:  s,
			Message: message,
		}

		s.hub.Recv(msg)
	}
}

// WritePump starts the writer loop for the socket.
func (c *Socket) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.conn.Close()
		c.Wait.Done()
	}()

	c.Wait.Add(1)

	for {
		select {
		case message, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := c.conn.NextWriter(websocket.TextMessage)
			if err != nil {
				log.Printf("failed to retrieve writer: %v", err)
				return
			}

			w.Write(message)

			if err := w.Close(); err != nil {
				log.Printf("failed to close writer: %v", err)
				return
			}
		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				log.Printf("failed to send ping: %v", err)
				return
			}
		}
	}
}

// Send allows sending a serialised message to the socket.
func (c *Socket) Send(msg []byte) {
	log.Printf("sending message to %s", c.conn.RemoteAddr())
	c.send <- msg
}

// SendError is a utility method that allows sending an `error` as a `message.Error`
// message to the socket.
func (c *Socket) SendError(id int, err error) {
	msgError := &message.Error{}

	if xerrors.As(err, &msgError) {
		answer := message.Answer{
			ID:    &id,
			Error: msgError,
		}

		answerBuf, err := json.Marshal(answer)
		if err != nil {
			log.Printf("failed to marshal answer: %v", err)
		}

		c.send <- answerBuf
	}
}

// SendResult is a utility method that allows sending a `message.Result` to the socket.
func (c *Socket) SendResult(id int, res message.Result) {
	answer := message.Answer{
		ID:     &id,
		Result: &res,
	}

	answerBuf, err := json.Marshal(answer)
	if err != nil {
		log.Printf("failed to marshal answer: %v", err)
	}

	log.Printf("answerBuf: %s, received id: %d", answerBuf, id)
	c.send <- answerBuf
}
