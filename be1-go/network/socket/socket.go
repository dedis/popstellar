package socket

import (
	"encoding/json"
	"log"
	"student20_pop/message"
	"sync"
	"time"

	messageX "student20_pop/message2/query/method/message"

	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"golang.org/x/xerrors"
)

// SocketType represents different socket types
type SocketType string

const (
	// ClientSocketType denotes a client.
	ClientSocketType SocketType = "client"

	//OrganizerSocketType denotes an organizer.
	OrganizerSocketType SocketType = "organizer"

	// WitnessSocketType denotes a witness.
	WitnessSocketType SocketType = "witness"
)

// baseSocket represents a socket connected to the server.
type baseSocket struct {
	id string

	socketType SocketType

	receiver chan<- IncomingMessage

	// Used to remove sockets which close unexpectedly.
	closedSockets chan<- string

	conn *websocket.Conn

	send chan []byte

	wg *sync.WaitGroup

	done chan struct{}
}

func (s *baseSocket) ID() string {
	return s.id
}

func (s *baseSocket) Type() SocketType {
	return s.socketType
}

// ReadPump starts the reader loop for the socket.
func (s *baseSocket) ReadPump() {
	defer func() {
		s.conn.Close()
		s.wg.Done()
		// it's safe to send a message on s.closedSockets after calling s.wg.Done()
		// If the hub is still open then it will be processed an the client will be
		// unsubscribed. Otherwise, since the hub is being shut down, this won't
		// block because the process will exit.
		s.closedSockets <- s.ID()
	}()

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

		msg := IncomingMessage{
			Socket:  s,
			Message: message,
		}

		s.receiver <- msg

		// return if we're done
		select {
		case <-s.done:
			return
		default:
		}
	}
}

// WritePump starts the writer loop for the socket.
func (s *baseSocket) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		s.conn.Close()
		s.wg.Done()
		// it's safe to send a message on s.closedSockets after calling s.wg.Done()
		// If the hub is still open then it will be processed an the client will be
		// unsubscribed. Otherwise, since the hub is being shut down, this won't
		// block because the process will exit.
		s.closedSockets <- s.ID()
	}()

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
		case <-s.done:
			log.Println("closing the write pump")
			s.conn.WriteMessage(websocket.CloseGoingAway, []byte{})
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
func (s *baseSocket) SendResult(id int, res []messageX.Message) {
	var answer interface{}

	if res == nil {
		answer = struct {
			JSONRPC string `json:"jsonrpc"`
			ID      int    `json:"id"`
			Result  int    `json:"result"`
		}{
			"2.0", id, 0,
		}
	} else {
		for _, r := range res {
			if r.WitnessSignatures == nil {
				r.WitnessSignatures = []messageX.WitnessSignature{}
			}
		}
		answer = struct {
			JSONRPC string             `json:"jsonrpc"`
			ID      int                `json:"id"`
			Result  []messageX.Message `json:"result"`
		}{
			"2.0", id, res,
		}
	}

	answerBuf, err := json.Marshal(&answer)
	if err != nil {
		log.Printf("failed to marshal answer: %v", err)
	}

	log.Printf("answerBuf: %s, received id: %d", answerBuf, id)
	s.send <- answerBuf
}

func newBaseSocket(socketType SocketType, receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup, done chan struct{}) *baseSocket {
	return &baseSocket{
		id:            xid.New().String(),
		socketType:    socketType,
		receiver:      receiver,
		closedSockets: closedSockets,
		conn:          conn,
		send:          make(chan []byte, 256),
		wg:            wg,
		done:          done,
	}
}

// ClientSocket denotes a client socket and implements the Socket interface.
type ClientSocket struct {
	*baseSocket
}

// NewClient returns an instance of a baseSocket.
func NewClientSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup, done chan struct{}) *ClientSocket {
	return &ClientSocket{
		baseSocket: newBaseSocket(ClientSocketType, receiver, closedSockets, conn, wg, done),
	}
}

// OrganizerSocket denotes an organizer socket and implements the Socket interface.
type OrganizerSocket struct {
	*baseSocket
}

// NewOrganizerSocket returns a new OrganizerSocket.
func NewOrganizerSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup, done chan struct{}) *OrganizerSocket {
	return &OrganizerSocket{
		baseSocket: newBaseSocket(OrganizerSocketType, receiver, closedSockets, conn, wg, done),
	}
}

// WitnessSocket denotes a witness socket and implements the Socket interface.
type WitnessSocket struct {
	*baseSocket
}

// NewWitnessSocket returns a new WitnessSocket.
func NewWitnessSocket(receiver chan<- IncomingMessage, closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup, done chan struct{}) *WitnessSocket {
	return &WitnessSocket{
		baseSocket: newBaseSocket(WitnessSocketType, receiver, closedSockets, conn, wg, done),
	}
}
