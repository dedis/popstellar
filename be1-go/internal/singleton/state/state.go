package state

import (
	"github.com/rs/zerolog"
	"popstellar/internal/message/answer"
	"popstellar/internal/types/hubparams"
	"popstellar/internal/types/peers"
	"popstellar/internal/types/queries"
	"popstellar/internal/types/sockets"
	types2 "popstellar/internal/types/subscribers"
	"sync"
)

var once sync.Once
var instance *state

type state struct {
	subs             Subscriber
	peers            Peerer
	queries          Querier
	hubParams        HubParameter
	sockets          Socketer
	resetRumorSender chan struct{}
}

func InitState(log *zerolog.Logger) {
	once.Do(func() {
		instance = &state{
			subs:             types2.NewSubscribers(),
			peers:            peers.NewPeers(),
			queries:          queries.NewQueries(log),
			hubParams:        hubparams.NewHubParams(),
			sockets:          sockets.NewSockets(),
			resetRumorSender: make(chan struct{}),
		}
	})
}

// ONLY FOR TEST PURPOSE
// SetState is only here to be used to reset the state before each test
func SetState(subs Subscriber, peers Peerer, queries Querier, hubParams HubParameter) {
	instance = &state{
		subs:      subs,
		peers:     peers,
		queries:   queries,
		hubParams: hubParams,
	}
}

func GetResetRumorSender() (chan struct{}, *answer.Error) {
	if instance == nil || instance.resetRumorSender == nil {
		return nil, answer.NewInternalServerError("resetRumorSender was not instantiated")
	}

	return instance.resetRumorSender, nil
}

func NotifyResetRumorSender() *answer.Error {
	if instance == nil || instance.resetRumorSender == nil {
		return answer.NewInternalServerError("resetRumorSender was not instantiated")
	}

	select {
	case instance.resetRumorSender <- struct{}{}:
	case <-instance.hubParams.GetStopChan():
	}

	return nil
}
