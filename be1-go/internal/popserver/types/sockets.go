package types

import (
	"popstellar/network/socket"
	"sync"
)

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

// Len returns the number of Sockets.
func (s *Sockets) Len() int {
	return len(s.store)
}

// SendToAll sends a message to all Sockets.
func (s *Sockets) SendToAll(buf []byte) {
	s.RLock()
	defer s.RUnlock()

	for _, s := range s.store {
		s.Send(buf)
	}
}

// Upsert upserts a socket into the Sockets store.
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
