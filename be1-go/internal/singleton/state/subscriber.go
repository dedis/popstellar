package state

import (
	"popstellar/internal/errors"
	"popstellar/internal/network/socket"
)

type Subscriber interface {
	AddChannel(channelPath string) error
	HasChannel(channelPath string) bool
	Subscribe(channelPath string, socket socket.Socket) error
	Unsubscribe(channelPath string, socket socket.Socket) error
	UnsubscribeFromAll(socketID string)
	SendToAll(buf []byte, channelPath string) error
}

func getSubs() (Subscriber, error) {
	if instance == nil || instance.subs == nil {
		return nil, errors.NewInternalServerError("subscriber was not instantiated")
	}

	return instance.subs, nil
}

func AddChannel(channelPath string) error {
	subs, err := getSubs()
	if err != nil {
		return err
	}

	return subs.AddChannel(channelPath)
}

func HasChannel(channelPath string) (bool, error) {
	subs, err := getSubs()
	if err != nil {
		return false, err
	}

	return subs.HasChannel(channelPath), nil
}

func Subscribe(socket socket.Socket, channelPath string) error {
	subs, err := getSubs()
	if err != nil {
		return err
	}

	return subs.Subscribe(channelPath, socket)
}

func Unsubscribe(socket socket.Socket, channelPath string) error {
	subs, err := getSubs()
	if err != nil {
		return err
	}

	return subs.Unsubscribe(channelPath, socket)
}

func UnsubscribeFromAll(socketID string) error {
	subs, err := getSubs()
	if err != nil {
		return err
	}

	subs.UnsubscribeFromAll(socketID)

	return nil
}

func SendToAll(buf []byte, channelPath string) error {
	subs, err := getSubs()
	if err != nil {
		return err
	}

	return subs.SendToAll(buf, channelPath)
}
