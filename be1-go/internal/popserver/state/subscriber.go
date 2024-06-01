package state

import (
	"popstellar/message/answer"
	"popstellar/network/socket"
)

type Subscriber interface {
	AddChannel(channel string) *answer.Error
	HasChannel(channel string) bool
	Subscribe(channel string, socket socket.Socket) *answer.Error
	Unsubscribe(channel string, socket socket.Socket) *answer.Error
	UnsubscribeFromAll(socketID string)
	SendToAll(buf []byte, channel string) *answer.Error
}

func getSubs() (Subscriber, *answer.Error) {
	if instance == nil || instance.subs == nil {
		return nil, answer.NewInternalServerError("subscriber was not instantiated")
	}

	return instance.subs, nil
}

func AddChannel(channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.AddChannel(channel)
}

func HasChannel(channel string) (bool, *answer.Error) {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return false, errAnswer
	}

	return subs.HasChannel(channel), nil
}

func Subscribe(socket socket.Socket, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.Subscribe(channel, socket)
}

func Unsubscribe(socket socket.Socket, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.Unsubscribe(channel, socket)
}

func UnsubscribeFromAll(socketID string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	subs.UnsubscribeFromAll(socketID)

	return nil
}

func SendToAll(buf []byte, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.SendToAll(buf, channel)
}
