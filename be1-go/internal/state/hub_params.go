package state

import (
	"github.com/rs/zerolog"
	"popstellar/internal/network/socket"
	"sync"
)

type HubParams struct {
	wg               sync.WaitGroup
	messageChan      chan socket.IncomingMessage
	closedSockets    chan string
	stop             chan struct{}
	resetRumorSender chan struct{}
	log              zerolog.Logger
}

func NewHubParams(log zerolog.Logger) *HubParams {
	return &HubParams{
		messageChan:      make(chan socket.IncomingMessage, 100),
		stop:             make(chan struct{}),
		closedSockets:    make(chan string),
		resetRumorSender: make(chan struct{}),
		log:              log.With().Str("module", "hub_params").Logger(),
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

func (h *HubParams) GetResetRumorSender() chan struct{} {
	return h.resetRumorSender
}

func (h *HubParams) NotifyResetRumorSender() error {
	select {
	case h.resetRumorSender <- struct{}{}:
	case <-h.stop:
	}

	return nil
}
