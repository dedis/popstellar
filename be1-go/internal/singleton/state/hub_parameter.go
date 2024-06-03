package state

import (
	"popstellar/internal/errors"
	"popstellar/internal/network/socket"
	"sync"
)

type HubParameter interface {
	GetWaitGroup() *sync.WaitGroup
	GetMessageChan() chan socket.IncomingMessage
	GetStopChan() chan struct{}
	GetClosedSockets() chan string
}

func getHubParams() (HubParameter, error) {
	if instance == nil || instance.hubParams == nil {
		return nil, errors.NewInternalServerError("hubparams was not instantiated")
	}

	return instance.hubParams, nil
}

func GetWaitGroup() (*sync.WaitGroup, error) {
	hubParams, err := getHubParams()
	if err != nil {
		return nil, err
	}

	return hubParams.GetWaitGroup(), nil
}

func GetMessageChan() (chan socket.IncomingMessage, error) {
	hubParams, err := getHubParams()
	if err != nil {
		return nil, err
	}

	return hubParams.GetMessageChan(), nil
}

func GetStopChan() (chan struct{}, error) {
	hubParams, err := getHubParams()
	if err != nil {
		return nil, err
	}

	return hubParams.GetStopChan(), nil
}

func GetClosedSockets() (chan string, error) {
	hubParams, err := getHubParams()
	if err != nil {
		return nil, err
	}

	return hubParams.GetClosedSockets(), nil
}
