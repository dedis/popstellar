package types

import (
	"popstellar"
	"popstellar/network/socket"
	"sync"
)

// NewSockets returns a new initialized Sockets
func NewSockets() Sockets {
	return Sockets{
		rumorFirstSocket:      make(map[int]string),
		nextSocketToSendRumor: 0,
		socketIDs:             make([]string, 0),
		store:                 make(map[string]socket.Socket),
	}
}

// Sockets provides thread-functionalities around a socket store.
type Sockets struct {
	sync.RWMutex
	rumorFirstSocket      map[int]string
	nextSocketToSendRumor int
	socketIDs             []string
	store                 map[string]socket.Socket
}

// Len returns the number of Sockets.
func (s *Sockets) Len() int {
	return len(s.store)
}

// SendToAll sends a message to all Sockets.
func (s *Sockets) SendToAll(buf []byte) {
	s.RLock()
	defer s.RUnlock()

	for _, v := range s.store {
		v.Send(buf)
	}
}

func (s *Sockets) SendRumor(rumorID int, buf []byte) {
	s.Lock()
	defer s.Unlock()

	if len(s.store) == 0 {
		return
	}

	socketID := s.socketIDs[s.nextSocketToSendRumor]

	firstSocketID, ok := s.rumorFirstSocket[rumorID]
	if !ok {
		s.rumorFirstSocket[rumorID] = socketID
	} else if firstSocketID == socketID {
		popstellar.Logger.Debug().Msgf("stop sending rumor because completed cycle")
		return
	}

	s.nextSocketToSendRumor = (s.nextSocketToSendRumor + 1) % len(s.socketIDs)

	s.store[socketID].Send(buf)
}

// Upsert upserts a socket into the Sockets store.
func (s *Sockets) Upsert(socket socket.Socket) {
	s.Lock()
	defer s.Unlock()

	s.socketIDs = append(s.socketIDs, socket.ID())
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

	index := -1

	for i, socketID := range s.socketIDs {
		if socketID == ID {
			index = i
		}
	}

	if index == -1 {
		return false
	}

	socketIDs := make([]string, 0)
	socketIDs = append(socketIDs, s.socketIDs[:index]...)
	s.socketIDs = append(socketIDs, s.socketIDs[index+1:]...)

	return true
}
