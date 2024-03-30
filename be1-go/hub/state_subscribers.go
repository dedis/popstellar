package hub

import (
	"popstellar/message/answer"
	"popstellar/network/socket"
)

type subscribers map[string]map[string]socket.Socket

func (s subscribers) addChannel(channel string) {
	s[channel] = make(map[string]socket.Socket)
}

func (s subscribers) removeChannel(channel string) {
	delete(s, channel)
}

func (s subscribers) subscribe(channel string, socket socket.Socket) *answer.Error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot subscribe to unknown channel")
	}

	s[channel][socket.ID()] = socket

	return nil
}

func (s subscribers) unsubscribe(channel string, socket socket.Socket) *answer.Error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot unsubscribe from unknown channel")
	}

	_, ok = s[channel][socket.ID()]
	if !ok {
		return answer.NewInvalidActionError("cannot unsubscribe from a channel not subscribed")
	}

	delete(s[channel], socket.ID())

	return nil
}

// SendToAll sends a message to all sockets.
func (s subscribers) SendToAll(buf []byte, channel string) *answer.Error {

	sockets, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("channel %s not found", channel)
	}
	for _, v := range sockets {
		v.Send(buf)
	}
	return nil
}
