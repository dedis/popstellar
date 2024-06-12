package types

import (
	"popstellar/internal/errors"
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

func (s *Subscribers) AddChannel(channelPath string) error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channelPath]
	if ok {
		return errors.NewDuplicateResourceError("channel %s already exists", channelPath)
	}

	s.list[channelPath] = make(map[string]socket.Socket)

	return nil
}

func (s *Subscribers) Subscribe(channelPath string, socket socket.Socket) error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channelPath]
	if !ok {
		return errors.NewInvalidResourceError("cannot Subscribe to unknown channel: %s", channelPath)
	}

	s.list[channelPath][socket.ID()] = socket

	return nil
}

func (s *Subscribers) Unsubscribe(channelPath string, socket socket.Socket) error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channelPath]
	if !ok {
		return errors.NewInvalidResourceError("cannot Unsubscribe from unknown channel: %s", channelPath)
	}

	_, ok = s.list[channelPath][socket.ID()]
	if !ok {
		return errors.NewInvalidActionError("cannot Unsubscribe from a channel not subscribed: %s", channelPath)
	}

	delete(s.list[channelPath], socket.ID())

	return nil
}

func (s *Subscribers) UnsubscribeFromAll(socketID string) {
	s.Lock()
	defer s.Unlock()

	for channelPath, subs := range s.list {
		_, ok := subs[socketID]
		if !ok {
			continue
		}
		delete(s.list[channelPath], socketID)
	}
}

// SendToAll sends a message to all sockets.
func (s *Subscribers) SendToAll(buf []byte, channelPath string) error {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channelPath]
	if !ok {
		return errors.NewInvalidResourceError("failed to send to all clients, channel %s not found", channelPath)
	}
	for _, v := range sockets {
		v.Send(buf)
	}

	return nil
}

func (s *Subscribers) HasChannel(channelPath string) bool {
	s.RLock()
	defer s.RUnlock()

	_, ok := s.list[channelPath]

	return ok
}

func (s *Subscribers) IsSubscribed(channelPath string, socket socket.Socket) (bool, error) {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channelPath]
	if !ok {
		return false, errors.NewInvalidResourceError("channel doesn't exist: %s", channelPath)
	}
	_, ok = sockets[socket.ID()]
	if !ok {
		return false, nil
	}

	return true, nil
}