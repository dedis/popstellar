package channel

import (
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"

	"github.com/rs/zerolog"

	"go.dedis.ch/kyber/v3"
)

// LaoFactory is the function passed to the hub that it must use to
// create a new lao channel.
type LaoFactory func(channelID string, hub HubFunctionalities, msg message.Message,
	log zerolog.Logger, organizerKey kyber.Point, socket socket.Socket) (Channel, error)

// Channel represents a PoP channel - like a LAO.
type Channel interface {
	// Subscribe is used to handle a subscribe message.
	Subscribe(socket socket.Socket, msg method.Subscribe) error

	// Unsubscribe is used to handle an unsubscribe message.
	Unsubscribe(socketID string, msg method.Unsubscribe) error

	// Publish is used to handle a publish message. The sender's socket may be
	// needed when a message creates a channel, to know if the server should
	// catchup on this channel or not.
	Publish(msg method.Publish, socket socket.Socket) error

	// Catchup is used to handle a catchup message.
	Catchup(msg method.Catchup) []message.Message

	// Broadcast is used to handle a broadcast message.
	Broadcast(msg method.Broadcast, socket socket.Socket) error
}

// NewSockets returns a new initialized Sockets
func NewSockets() Sockets {
	return Sockets{
		store: make(map[string]socket.Socket),
	}
}

// Sockets provides thread-functionalities around a socket store.
type Sockets struct {
	sync.RWMutex
	store map[string]socket.Socket
}

// Len returns the number of sockets.
func (s *Sockets) Len() int {
	return len(s.store)
}

// SendToAll sends a message to all sockets.
func (s *Sockets) SendToAll(buf []byte) {
	s.RLock()
	defer s.RUnlock()

	for _, s := range s.store {
		s.Send(buf)
	}
}

// Upsert upserts a socket into the sockets store.
func (s *Sockets) Upsert(socket socket.Socket) {
	s.Lock()
	defer s.Unlock()

	s.store[socket.ID()] = socket
}

// Delete deletes a socket from the store. Returns false
// if the socket is not present in the store and true
// on success.
func (s *Sockets) Delete(ID string) bool {
	s.Lock()
	defer s.Unlock()

	_, ok := s.store[ID]
	if !ok {
		return false
	}

	delete(s.store, ID)

	return true
}

// HubFunctionalities defines the functions needed by a channel from the hub.
type HubFunctionalities interface {
	GetPubKeyOwner() kyber.Point
	GetPubKeyServ() kyber.Point
	Sign([]byte) ([]byte, error)
	GetSchemaValidator() validation.SchemaValidator
	NotifyNewChannel(channelID string, channel Channel, socket socket.Socket)
	GetServerNumber() int
	SendAndHandleMessage(method.Broadcast) error
	GetServerAddress() string
}

// Broadcastable defines a channel that can broadcast
type Broadcastable interface {
	Broadcast(msg method.Broadcast, _ socket.Socket) error
	GetChannelPath() string
}

// LAOFunctionalities defines the functions needed by the LAO from another channel.
type LAOFunctionalities interface {
	AddAttendee(string)
}
