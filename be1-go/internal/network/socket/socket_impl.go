package socket

import (
	"encoding/json"
	"errors"
	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	poperror "popstellar/internal/errors"
	"popstellar/internal/handler/answer/manswer"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"sync"
	"time"
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
				s.log.Err(err).Msg("connection dropped unexpectedly")
			} else {
				s.log.Info().Msg("closing the read pump")
			}
			break
		}

		s.log.Info().RawJSON("received", message).Msg("")
		msg := IncomingMessage{
			Socket:  s,
			Message: message,
		}

		// return if we're done
		select {
		case <-s.done:
			return
		case s.receiver <- msg:
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
			s.log.Info().RawJSON("sent", message).Msg("")

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
	s.send <- msg
}

// SendError is a utility method that allows sending an `error` as a
// `message.Error` message to the socket.
func (s *baseSocket) SendError(id *int, err error) {
	msgError := &manswer.Error{}

	if !errors.As(err, &msgError) {
		msgError = manswer.NewError(-6, err.Error())
	}

	answer := manswer.Answer{
		JSONRPCBase: mjsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		ID:    id,
		Error: msgError,
	}

	answerBuf, err := json.Marshal(answer)
	if err != nil {
		s.log.Err(err).Msg("failed to marshal error")
		return
	}

	s.send <- answerBuf
}

// SendError is a utility method that allows sending an `error` as a
// `message.Error` message to the socket.
func (s *baseSocket) SendPopError(id *int, err error) {
	popError := &poperror.PopError{}

	if !errors.As(err, &popError) {
		popError = poperror.NewPopError(poperror.InternalServerErrorCode, err.Error())
	}

	description := popError.Error() + "\n" + popError.StackTraceString()

	msgError := manswer.Error{
		Code:        popError.Code(),
		Description: description,
	}

	answer := manswer.Answer{
		JSONRPCBase: mjsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		ID:    id,
		Error: &msgError,
	}

	answerBuf, err := json.Marshal(answer)
	if err != nil {
		s.log.Err(err).Msg("failed to marshal poperror")
		return
	}

	s.send <- answerBuf
}

// SendResult is a utility method that allows sending a `message.Result` to the
// socket.
func (s *baseSocket) SendResult(id int, res []mmessage.Message, missingMessagesByChannel map[string][]mmessage.Message) {
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
				r.WitnessSignatures = []mmessage.WitnessSignature{}
			}
		}
		answer = struct {
			JSONRPC string             `json:"jsonrpc"`
			ID      int                `json:"id"`
			Result  []mmessage.Message `json:"result"`
		}{
			"2.0", id, res,
		}
	} else if missingMessagesByChannel != nil {
		answer = struct {
			JSONRPC string                        `json:"jsonrpc"`
			ID      int                           `json:"id"`
			Result  map[string][]mmessage.Message `json:"result"`
		}{
			"2.0", id, missingMessagesByChannel,
		}
	}

	answerBuf, err := json.Marshal(&answer)
	if err != nil {
		s.log.Err(err).Msg("failed to marshal result")
		return
	}

	s.send <- answerBuf
}

func newBaseSocket(socketType SocketType, receiver chan<- IncomingMessage,
	closedSockets chan<- string, conn *websocket.Conn, wg *sync.WaitGroup,
	done chan struct{}, log zerolog.Logger,
) *baseSocket {
	id := xid.New().String()

	if conn != nil {
		log = log.With().Dict("socket", zerolog.Dict().
			Str("ID", id).
			Str("IP", conn.RemoteAddr().String())).Logger()
	} else {
		log = log.With().Str("socketID", id).Logger()
	}

	return &baseSocket{
		id:            id,
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
	done chan struct{}, log zerolog.Logger,
) *ClientSocket {
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
	done chan struct{}, log zerolog.Logger,
) *ServerSocket {
	return &ServerSocket{
		baseSocket: newBaseSocket(ServerSocketType, receiver, closedSockets,
			conn, wg, done, log),
	}
}
