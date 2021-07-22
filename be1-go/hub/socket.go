package hub

import (
	"context"
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
	maxMessageSize = 256 * 1024 // 256K

	// writeWait denotes the timeout for writing.
	writeWait = 10 * time.Second

	// pongWait is the timeout for reading a pong.
	pongWait = 60 * time.Second

	// pingPeriod is the interval to send ping messages in.
	pingPeriod = (pongWait * 9) / 10
)

// Socket is an interface which allows reading/writing messages to
// another client
type Socket interface {
	// Type denotes the type of socket.
	Type() string

	// ReadPump is a lower level method for reading messages from the socket.
	// TODO: this probably shouldn't be a part of the interface
	ReadPump()

	// WritePump is a lower level method for writing messages to the socket.
	// TODO: this probably shouldn't be a part of the interface
	WritePump()

	// Send is used to send a message to the client.
	Send(msg []byte)

	// SendError is used to send an error to the client.
	// Please refer to the Protocol Specification document for information
	// on the error codes.
	SendError(id int, err error)

	// SendResult is used to send a result message to the client.
	SendResult(id int, res message.Result)
}

// baseSocket represents a socket connected to the server.
type baseSocket struct {
	socketType SocketType

	hub Hub

	conn *websocket.Conn

	send chan []byte

	Wait *sync.WaitGroup
}

func (s *baseSocket) Type() SocketType {
	return s.socketType
}

// ReadPump starts the reader loop for the socket.
func (s *baseSocket) ReadPump(ctx context.Context) {
	defer func() {
		s.conn.Close()
		s.Wait.Done()
	}()

	s.Wait.Add(1)

	log.Printf("listening for messages from %s", s.socketType)

	s.conn.SetReadLimit(maxMessageSize)
	s.conn.SetReadDeadline(time.Now().Add(pongWait))
	s.conn.SetPongHandler(func(string) error { s.conn.SetReadDeadline(time.Now().Add(pongWait)); return nil })
	for {
		_, message, err := s.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("connection dropped unexpectedly: %v", err)
			} else {
				log.Printf("closing the read pump")
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

		if ctx.Err() != nil {
			log.Println("closing the read pump")
			return
		}
	}
}

// WritePump starts the writer loop for the socket.
func (s *baseSocket) WritePump(ctx context.Context) {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		s.conn.Close()
		s.Wait.Done()
	}()

	s.Wait.Add(1)

	for {
		select {
		case message, ok := <-s.send:
			s.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				s.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := s.conn.NextWriter(websocket.TextMessage)
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
			s.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := s.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				log.Printf("failed to send ping: %v", err)
				return
			}
		case <-ctx.Done():
			log.Println("closing the write pump")
			return
		}
	}
}

// Send allows sending a serialised message to the socket.
func (s *baseSocket) Send(msg []byte) {
	log.Printf("sending message to %s", s.conn.RemoteAddr())
	s.send <- msg
}

// SendError is a utility method that allows sending an `error` as a `message.Error`
// message to the socket.
func (s *baseSocket) SendError(id *int, err error) {
	log.Printf("Error: %v", err)
	msgError := &message.Error{}

	if xerrors.As(err, &msgError) {
		answer := message.Answer{
			ID:    id,
			Error: msgError,
		}

		answerBuf, err := json.Marshal(answer)
		if err != nil {
			log.Printf("failed to marshal answer: %v", err)
		}

		s.send <- answerBuf
	}
}

// SendResult is a utility method that allows sending a `message.Result` to the socket.
func (s *baseSocket) SendResult(id int, res message.Result) {
	answer := message.Answer{
		ID:     &id,
		Result: &res,
	}

	answerBuf, err := json.Marshal(answer)
	if err != nil {
		log.Printf("failed to marshal answer: %v", err)
	}

	log.Printf("answerBuf: %s, received id: %d", answerBuf, id)
	s.send <- answerBuf
}
