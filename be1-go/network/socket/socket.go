package socket

import (
	"encoding/json"
	jsonrpc "popstellar/message"

	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

// SocketType represents different socket types
type SocketType string

const (
	// ClientSocketType denotes a client.
	ClientSocketType SocketType = "client"

	// ServerSocketType denotes a server.
	ServerSocketType SocketType = "server"
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

	log zerolog.Logger
}

func (s *baseSocket) ID() string {
	return s.id
}

func (s *baseSocket) Type() SocketType {
	return s.socketType
}

func (s *baseSocket) Address() string {
	s.log.Info().
		Str("is", s.conn.RemoteAddr().String()).
		Msg("Peer remote address")
	s.log.Info().
		Str("is", s.conn.LocalAddr().String()).
		Msg("Peer local address")
	return s.conn.RemoteAddr().String()
}

// ReadPump starts the reader loop for the socket.
func (s *baseSocket) ReadPump() {
	defer func() {
		s.conn.Close()
		s.wg.Done()

		// it is safe to send a message on s.closedSockets after calling
		// s.wg.Done() If the hub is still open then it will be processed and
		// the client will be unsubscribed. Otherwise, since the hub is being
		// shut down, this won't block because the process will exit.
		s.closedSockets <- s.ID()
	}()

	s.log.Info().Msgf("listening for messages from %s", s.socketType)

	s.conn.SetReadLimit(maxMessageSize)
	s.conn.SetReadDeadline(time.Now().Add(pongWait))
	s.conn.SetPongHandler(func(string) error {
		s.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := s.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				s.log.Err(err).
					Str("socket", s.conn.RemoteAddr().String()).
					Msg("connection dropped unexpectedly")
			} else {
				s.log.Info().Msg("closing the read pump")
			}
			break
		}

		msg := IncomingMessage{
			Socket:  s,
			Message: message,
		}

		// return if we're done
		select {
		case <-s.done:
			return
		default:
			s.receiver <- msg
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
		// it's safe to send a message on s.closedSockets after calling
		// s.wg.Done() If the hub is still open then it will be processed and
		// the client will be unsubscribed. Otherwise, since the hub is being
		// shut down, this won't block because the process will exit.
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
				s.log.Err(err).Msg("failed to retrieve writer")
				return
			}

			w.Write(message)

			if err := w.Close(); err != nil {
				s.log.Err(err).Msg("failed to close writer")
				return
			}
		case <-ticker.C:
			s.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := s.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				s.log.Err(err).Msg("failed to send ping")
				return
			}
		case <-s.done:
			s.log.Info().Msg("closing the write pump")
			s.conn.WriteMessage(websocket.CloseGoingAway, []byte{})
			return
		}
	}
}

// Send allows sending a serialized message to the socket.
func (s *baseSocket) Send(msg []byte) {
	s.log.Info().
		Str("to", s.conn.RemoteAddr().String()).
		Str("msg", string(msg)).
		Msg("send generic msg")
	s.send <- msg
}

// SendError is a utility method that allows sending an `error` as a
// `message.Error` message to the socket.
func (s *baseSocket) SendError(id *int, err error) {
	msgError := &answer.Error{}

	if !xerrors.As(err, &msgError) {
		msgError = answer.NewError(-6, err.Error())
	}

	answer := answer.Answer{
		JSONRPCBase: jsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		ID:    id,
		Error: msgError,
	}

	answerBuf, err := json.Marshal(answer)
	if err != nil {
		s.log.Err(err).Msg("failed to marshal answer")
		return
	}

	s.log.Info().
		Str("to", s.conn.RemoteAddr().String()).
		Str("msg", string(answerBuf)).
		Msg("send error")

	s.send <- answerBuf
}

// SendResult is a utility method that allows sending a `message.Result` to the
// socket.
func (s *baseSocket) SendResult(id int, res []message.Message, missingMessagesByChannel map[string][]message.Message) {
	var answer interface{}

	if res != nil && missingMessagesByChannel != nil {
		s.log.Error().Msg("The result must be either a slice or a map of messages, not both.")
		return
	}

	if res == nil && missingMessagesByChannel == nil {
		answer = struct {
			JSONRPC string `json:"jsonrpc"`
			ID      int    `json:"id"`
			Result  int    `json:"result"`
		}{
			"2.0", id, 0,
		}
	} else if res != nil {
		for _, r := range res {
			if r.WitnessSignatures == nil {
				r.WitnessSignatures = []message.WitnessSignature{}
			}
		}
		answer = struct {
			JSONRPC string            `json:"jsonrpc"`
			ID      int               `json:"id"`
			Result  []message.Message `json:"result"`
		}{
			"2.0", id, res,
		}
	} else if missingMessagesByChannel != nil {
		answer = struct {
			JSONRPC string                       `json:"jsonrpc"`
			ID      int                          `json:"id"`
			Result  map[string][]message.Message `json:"result"`
		}{
			"2.0", id, missingMessagesByChannel,
		}
	}

	answerBuf, err := json.Marshal(&answer)
	if err != nil {
		s.log.Err(err).Msg("failed to marshal answer")
		return
	}

	s.log.Info().
		Str("to", s.id).
		Str("msg", string(answerBuf)).
		Msg("send result")
	s.send <- answerBuf
}

func newBaseSocket(socketType SocketType, receiver chan<- IncomingMessage,
	closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup,
	done chan struct{}, log zerolog.Logger) *baseSocket {

	return &baseSocket{
		id:            xid.New().String(),
		socketType:    socketType,
		receiver:      receiver,
		closedSockets: closedSockets,
		conn:          conn,
		send:          make(chan []byte, 256),
		wg:            wg,
		done:          done,
		log:           log,
	}
}

// ClientSocket denotes a client socket and implements the Socket interface.
type ClientSocket struct {
	*baseSocket
}

// NewClientSocket returns an instance of a baseSocket.
func NewClientSocket(receiver chan<- IncomingMessage,
	closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup,
	done chan struct{}, log zerolog.Logger) *ClientSocket {

	log = log.With().Str("role", "client socket").Logger()

	return &ClientSocket{
		baseSocket: newBaseSocket(ClientSocketType, receiver, closedSockets,
			conn, wg, done, log),
	}
}

// ServerSocket denotes an organizer socket and implements the Socket interface.
type ServerSocket struct {
	*baseSocket
}

// NewServerSocket returns a new ServerSocket.
func NewServerSocket(receiver chan<- IncomingMessage,
	closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup,
	done chan struct{}, log zerolog.Logger) *ServerSocket {

	log = log.With().Str("role", "server socket").Logger()

	return &ServerSocket{
		baseSocket: newBaseSocket(ServerSocketType, receiver, closedSockets,
			conn, wg, done, log),
	}
}
