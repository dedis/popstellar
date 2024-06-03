package state

import (
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
)

type Peerer interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	AddPeerGreeted(socketID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

func getPeers() (Peerer, error) {
	if instance == nil || instance.peers == nil {
		return nil, errors.NewInternalServerError("peerer was not instantiated")
	}

	return instance.peers, nil
}

func AddPeerInfo(socketID string, info method.GreetServerParams) error {
	peers, err := getPeers()
	if err != nil {
		return err
	}

	return peers.AddPeerInfo(socketID, info)
}

func AddPeerGreeted(socketID string) error {
	peers, err := getPeers()
	if err != nil {
		return err
	}

	peers.AddPeerGreeted(socketID)

	return nil
}

func GetAllPeersInfo() ([]method.GreetServerParams, error) {
	peers, err := getPeers()
	if err != nil {
		return nil, err
	}

	return peers.GetAllPeersInfo(), nil
}

func IsPeerGreeted(socketID string) (bool, error) {
	peers, err := getPeers()
	if err != nil {
		return false, err
	}

	return peers.IsPeerGreeted(socketID), nil
}
