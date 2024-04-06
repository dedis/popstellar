package state

import (
	"popstellar/message/answer"
	"popstellar/network/socket"
)

type Subscribers map[string]map[string]socket.Socket

func (s Subscribers) AddChannel(channel string) {
	s[channel] = make(map[string]socket.Socket)
}

func (s Subscribers) Subscribe(channel string, socket socket.Socket) *answer.Error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Subscribe to unknown channel")
	}

	s[channel][socket.ID()] = socket

	return nil
}

func (s Subscribers) Unsubscribe(channel string, socket socket.Socket) *answer.Error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Unsubscribe from unknown channel")
	}

	_, ok = s[channel][socket.ID()]
	if !ok {
		return answer.NewInvalidActionError("cannot Unsubscribe from a channel not subscribed")
	}

	delete(s[channel], socket.ID())

	return nil
}

// SendToAll sends a message to all sockets.
func (s Subscribers) SendToAll(buf []byte, channel string) *answer.Error {

	sockets, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("failed to send to all clients, channel %s not found", channel)
	}
	for _, v := range sockets {
		v.Send(buf)
	}
	return nil
}
