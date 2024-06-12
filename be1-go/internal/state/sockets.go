package state

import (
	"fmt"
	"github.com/rs/zerolog"
	"math/rand"
	"popstellar/internal/network/socket"
	"sync"
)

// NewSockets returns a new initialized Sockets
func NewSockets(log zerolog.Logger) *Sockets {
	return &Sockets{
		rState:    make(map[string]rumorState),
		socketIDs: make([]string, 0),
		store:     make(map[string]socket.Socket),
		log:       log.With().Str("role", "sockets").Logger(),
	}
}

type rumorState struct {
	counter      int
	index        int
	bannedSocket string
}

// Sockets provides thread-functionalities around a socket store.
type Sockets struct {
	sync.RWMutex
	rState    map[string]rumorState
	socketIDs []string
	store     map[string]socket.Socket
	log       zerolog.Logger
}

func (s *Sockets) newRumorState(socket socket.Socket) rumorState {
	bannedSocket := ""

	if socket != nil {
		bannedSocket = socket.ID()
	}

	return rumorState{
		counter:      0,
		index:        rand.Intn(len(s.store)),
		bannedSocket: bannedSocket,
	}
}

// SendToAll sends a message to all Sockets.
func (s *Sockets) SendToAll(buf []byte) {
	s.RLock()
	defer s.RUnlock()

	for _, v := range s.store {
		v.Send(buf)
	}
}

func (s *Sockets) SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte) {
	s.Lock()
	defer s.Unlock()

	if len(s.store) == 0 {
		return
	}

	senderRumorID := fmt.Sprintf("%s%d", senderID, rumorID)

	rState, ok := s.rState[senderRumorID]
	if !ok {
		rState = s.newRumorState(socket)
		s.rState[senderRumorID] = rState
	} else {
		// to be sure to not overflow
		rState.index %= len(s.store)
		s.rState[senderRumorID] = rState
	}

	if s.socketIDs[rState.index] == rState.bannedSocket {
		rState.index += 1
		rState.index %= len(s.store)
		rState.counter += 1
		s.rState[senderRumorID] = rState
	}

	if rState.counter >= len(s.store) {
		s.log.Debug().Msgf("stop sending rumor because completed cycle")
		return
	}

	s.store[s.socketIDs[rState.index]].Send(buf)

	rState.index += 1
	rState.index %= len(s.store)
	rState.counter += 1
	s.rState[senderRumorID] = rState
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
