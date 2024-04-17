package state

import (
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"sync"
)

var once sync.Once
var instance *state

type state struct {
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
	once.Do(func() {
		instance = &state{
			subs:    subs,
			peers:   peers,
			queries: queries,
		}
	})
}

func GetSubsInstance() (Subscriber, bool) {
	if instance == nil || instance.subs == nil {
		return nil, false
	}

	return instance.subs, true
}

func GetPeersInstance() (Peerer, bool) {
	if instance == nil || instance.peers == nil {
		return nil, false
	}

	return instance.peers, true
}

func GetQueriesInstance() (Querier, bool) {
	if instance == nil || instance.queries == nil {
		return nil, false
	}

	return instance.queries, true
}
