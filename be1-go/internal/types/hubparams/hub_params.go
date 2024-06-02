package hubparams

import (
	"popstellar/internal/network/socket"
	"sync"
)

type HubParams struct {
	wg            sync.WaitGroup
	messageChan   chan socket.IncomingMessage
	closedSockets chan string
	stop          chan struct{}
}

func NewHubParams() *HubParams {
	return &HubParams{
		messageChan:   make(chan socket.IncomingMessage),
		stop:          make(chan struct{}),
		closedSockets: make(chan string),
	}
}

func (h *HubParams) GetWaitGroup() *sync.WaitGroup {
	return &h.wg
}

func (h *HubParams) GetMessageChan() chan socket.IncomingMessage {
	return h.messageChan
}

func (h *HubParams) GetStopChan() chan struct{} {
	return h.stop
}

func (h *HubParams) GetClosedSockets() chan string {
	return h.closedSockets
}
