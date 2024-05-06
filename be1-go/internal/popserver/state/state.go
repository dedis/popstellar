package state

import (
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"sync"
	"testing"
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
	AddPeerGreeted(socketID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

type Querier interface {
	GetQueryState(ID int) (bool, error)
	GetNextID() int
	SetQueryReceived(ID int) error
	AddQuery(ID int, query method.GetMessagesById)
}

func InitState(log *zerolog.Logger) {
	once.Do(func() {
		instance = &state{
			subs:    types.NewSubscribers(),
			peers:   types.NewPeers(),
			queries: types.NewQueries(log),
		}
	})
}

func SetState(t *testing.T, subs Subscriber, peers Peerer, queries Querier) error {
	if t == nil {
		return xerrors.Errorf("only for tests")
	}

	instance = &state{
		subs:    subs,
		peers:   peers,
		queries: queries,
	}

	return nil
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
