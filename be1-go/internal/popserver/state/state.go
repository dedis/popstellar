package state

import (
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"sync"
)

var popStateOnce sync.Once
var instancePopState *popState

type popState struct {
	subs    Subscriber
	peers   Peerer
	queries Querier
}

type Subscriber interface {
	AddChannel(channel string)
	Subscribe(channel string, socket socket.Socket) *answer.Error
	Unsubscribe(channel string, socket socket.Socket) *answer.Error
	SendToAll(buf []byte, channel string) *answer.Error
}

type Peerer interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	AddPeerGreeted(sockerID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

type Querier interface {
	GetQueryState(ID int) (bool, error)
	GetNextID() int
	SetQueryReceived(ID int) error
	AddQuery(ID int, query method.GetMessagesById)
}

func InitPopState(subs Subscriber, peers Peerer, queries Querier) {
	popStateOnce.Do(func() {
		instancePopState = &popState{
			subs:    subs,
			peers:   peers,
			queries: queries,
		}
	})
}

func GetSubsInstance() (Subscriber, bool) {
	if instancePopState == nil || instancePopState.subs == nil {
		return nil, false
	}

	return instancePopState.subs, true
}

func GetPeersInstance() (Peerer, bool) {
	if instancePopState == nil || instancePopState.peers == nil {
		return nil, false
	}

	return instancePopState.peers, true
}

func GetQueriesInstance() (Querier, bool) {
	if instancePopState == nil || instancePopState.queries == nil {
		return nil, false
	}

	return instancePopState.queries, true
}
