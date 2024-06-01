package state

import (
	"popstellar/message/answer"
	"popstellar/network/socket"
	"sync"
)

type HubParameter interface {
	GetWaitGroup() *sync.WaitGroup
	GetMessageChan() chan socket.IncomingMessage
	GetStopChan() chan struct{}
	GetClosedSockets() chan string
}

func getHubParams() (HubParameter, *answer.Error) {
	if instance == nil || instance.hubParams == nil {
		return nil, answer.NewInternalServerError("hubparams was not instantiated")
	}

	return instance.hubParams, nil
}

func GetWaitGroup() (*sync.WaitGroup, *answer.Error) {
	hubParams, errAnswer := getHubParams()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return hubParams.GetWaitGroup(), nil
}

func GetMessageChan() (chan socket.IncomingMessage, *answer.Error) {
	hubParams, errAnswer := getHubParams()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return hubParams.GetMessageChan(), nil
}

func GetStopChan() (chan struct{}, *answer.Error) {
	hubParams, errAnswer := getHubParams()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return hubParams.GetStopChan(), nil
}

func GetClosedSockets() (chan string, *answer.Error) {
	hubParams, errAnswer := getHubParams()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return hubParams.GetClosedSockets(), nil
}
