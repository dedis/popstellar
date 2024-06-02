package state

import (
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
)

type Peerer interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	AddPeerGreeted(socketID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

func getPeers() (Peerer, *answer.Error) {
	if instance == nil || instance.peers == nil {
		return nil, answer.NewInternalServerError("peerer was not instantiated")
	}

	return instance.peers, nil
}

func AddPeerInfo(socketID string, info method.GreetServerParams) *answer.Error {
	peers, errAnswer := getPeers()
	if errAnswer != nil {
		return errAnswer
	}

	err := peers.AddPeerInfo(socketID, info)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to add peer: %v", err)
		return errAnswer
	}

	return nil
}

func AddPeerGreeted(socketID string) *answer.Error {
	peers, errAnswer := getPeers()
	if errAnswer != nil {
		return errAnswer
	}

	peers.AddPeerGreeted(socketID)

	return nil
}

func GetAllPeersInfo() ([]method.GreetServerParams, *answer.Error) {
	peers, errAnswer := getPeers()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return peers.GetAllPeersInfo(), nil
}

func IsPeerGreeted(socketID string) (bool, *answer.Error) {
	peers, errAnswer := getPeers()
	if errAnswer != nil {
		return false, errAnswer
	}

	return peers.IsPeerGreeted(socketID), nil
}
