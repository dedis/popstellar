package types

import (
	"fmt"
	"golang.org/x/xerrors"
	"popstellar/internal/message/answer"
	"popstellar/internal/network/socket"
	"sync"
)

type Subscribers struct {
	sync.RWMutex
	list map[string]map[string]socket.Socket
}

func NewSubscribers() *Subscribers {
	return &Subscribers{
		list: make(map[string]map[string]socket.Socket),
	}
}

func (s *Subscribers) AddChannel(channel string) *answer.Error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channel]
	if ok {
		return answer.NewInvalidActionError("channel %s already exists", channel)
	}

	s.list[channel] = make(map[string]socket.Socket)

	return nil
}

func (s *Subscribers) Subscribe(channel string, socket socket.Socket) *answer.Error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Subscribe to unknown channel")
	}

	s.list[channel][socket.ID()] = socket

	return nil
}

func (s *Subscribers) Unsubscribe(channel string, socket socket.Socket) *answer.Error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Unsubscribe from unknown channel")
	}

	_, ok = s.list[channel][socket.ID()]
	if !ok {
		return answer.NewInvalidActionError("cannot Unsubscribe from a channel not subscribed")
	}

	delete(s.list[channel], socket.ID())

	return nil
}

func (s *Subscribers) UnsubscribeFromAll(socketID string) {
	s.Lock()
	defer s.Unlock()

	for channel, subs := range s.list {
		_, ok := subs[socketID]
		if !ok {
			continue
		}
		delete(s.list[channel], socketID)
		fmt.Println("unsubscribe from " + channel)
	}
}

// SendToAll sends a message to all sockets.
func (s *Subscribers) SendToAll(buf []byte, channel string) *answer.Error {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("failed to send to all clients, channel %s not found", channel)
	}
	for _, v := range sockets {
		v.Send(buf)
	}

	return nil
}

func (s *Subscribers) HasChannel(channel string) bool {
	s.RLock()
	defer s.RUnlock()

	_, ok := s.list[channel]

	return ok
}

func (s *Subscribers) IsSubscribed(channel string, socket socket.Socket) (bool, error) {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channel]
	if !ok {
		return false, xerrors.Errorf("channel doesn't exist")
	}
	_, ok = sockets[socket.ID()]
	if !ok {
		return false, nil
	}

	return true, nil
}
