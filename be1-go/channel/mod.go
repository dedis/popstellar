package channel

import (
	"github.com/rs/zerolog"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"

	"go.dedis.ch/kyber/v3"
)

// LaoFactory is the function passed to the organizer that it must use to
// create a new lao channel.
type LaoFactory func(channelID string, hub HubFunctionalities, msg message.Message, log zerolog.Logger) Channel

// Channel represents a PoP channel - like a LAO.
type Channel interface {
	// Subscribe is used to handle a subscribe message.
	Subscribe(socket socket.Socket, msg method.Subscribe) error

	// Unsubscribe is used to handle an unsubscribe message.
	Unsubscribe(socketID string, msg method.Unsubscribe) error

	// Publish is used to handle a publish message.
	Publish(msg method.Publish) error

	// Catchup is used to handle a catchup message.
	Catchup(msg method.Catchup) []message.Message
}

// NewSockets returns a new initialized sockets
func NewSockets() Sockets {
	return Sockets{
		store: make(map[string]socket.Socket),
	}
}

// Sockets provides thread-funcionalities around a socket store.
type Sockets struct {
	sync.RWMutex
	store map[string]socket.Socket
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
	GetPubkey() kyber.Point
	GetSchemaValidator() validation.SchemaValidator
	RegisterNewChannel(channelID string, channel Channel)
}
