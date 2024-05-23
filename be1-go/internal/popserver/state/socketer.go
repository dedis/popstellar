package state

import (
	"popstellar/message/answer"
	"popstellar/network/socket"
)

type Socketer interface {
	Len() int
	SendToAll(buf []byte)
	SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte)
	Upsert(socket socket.Socket)
	Delete(ID string) bool
}

func getSockets() (Socketer, *answer.Error) {
	if instance == nil || instance.sockets == nil {
		return nil, answer.NewInternalServerError("sockets was not instantiated")
	}

	return instance.sockets, nil
}

func Len() (int, *answer.Error) {
	sockets, errAnswer := getSockets()
	if errAnswer != nil {
		return -1, errAnswer
	}

	return sockets.Len(), nil
}

func SendToAllServer(buf []byte) *answer.Error {
	sockets, errAnswer := getSockets()
	if errAnswer != nil {
		return errAnswer
	}

	sockets.SendToAll(buf)

	return nil
}

func SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte) *answer.Error {
	sockets, errAnswer := getSockets()
	if errAnswer != nil {
		return errAnswer
	}

	sockets.SendRumor(socket, senderID, rumorID, buf)

	return nil
}

// Upsert upserts a socket into the Sockets store.
func Upsert(socket socket.Socket) *answer.Error {
	sockets, errAnswer := getSockets()
	if errAnswer != nil {
		return errAnswer
	}

	sockets.Upsert(socket)

	return nil
}

// Delete deletes a socket from the store. Returns false
// if the socket is not present in the store and true
// on success.
func Delete(ID string) (bool, *answer.Error) {
	sockets, errAnswer := getSockets()
	if errAnswer != nil {
		return false, errAnswer
	}

	return sockets.Delete(ID), nil
}
