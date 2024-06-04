package state

import (
	"popstellar/internal/errors"
	"popstellar/internal/network/socket"
)

type Socketer interface {
	SendToAll(buf []byte)
	SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte)
	Upsert(socket socket.Socket)
	Delete(ID string) bool
}

func getSockets() (Socketer, error) {
	if instance == nil || instance.sockets == nil {
		return nil, errors.NewInternalServerError("sockets was not instantiated")
	}

	return instance.sockets, nil
}

func SendToAllServer(buf []byte) error {
	sockets, err := getSockets()
	if err != nil {
		return err
	}

	sockets.SendToAll(buf)

	return nil
}

func SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte) error {
	sockets, err := getSockets()
	if err != nil {
		return err
	}

	sockets.SendRumor(socket, senderID, rumorID, buf)

	return nil
}

// Upsert upserts a socket into the Sockets store.
func Upsert(socket socket.Socket) error {
	sockets, err := getSockets()
	if err != nil {
		return err
	}

	sockets.Upsert(socket)

	return nil
}

// Delete deletes a socket from the store. Returns false
// if the socket is not present in the store and true
// on success.
func Delete(ID string) (bool, error) {
	sockets, err := getSockets()
	if err != nil {
		return false, err
	}

	return sockets.Delete(ID), nil
}
