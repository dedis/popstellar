package channel

import (
	"student20_pop/message/query/method"
	"student20_pop/message/query/method/message"
	"student20_pop/network/socket"
	"student20_pop/validation"
	"sync"

	"go.dedis.ch/kyber/v3"
)

// LaoFactory is the function passed to the organizer that it must use to
// create a new lao channel.
type LaoFactory func(channelID string, hub HubThingTheChannelNeeds, msg message.Message) Channel

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

// NewSockets ...
func NewSockets() Sockets {
	return Sockets{
		store: make(map[string]socket.Socket),
	}
}

// Sockets ...
type Sockets struct {
	sync.RWMutex
	store map[string]socket.Socket
}

// SendToAll ...
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

// MessageInfo ...
type MessageInfo struct {
	Message    message.Message
	StoredTime int64
}

// HubThingTheChannelNeeds ...
type HubThingTheChannelNeeds interface {
	GetPubkey() kyber.Point
	GetSchemaValidator() validation.SchemaValidator
	RegisterNewChannel(channelID string, channel Channel)
}
