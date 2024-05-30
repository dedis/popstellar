package state

import (
	"github.com/rs/zerolog"
	"popstellar/internal/popserver/types"
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
	AddChannel(channel string) *answer.Error
	HasChannel(channel string) bool
	Subscribe(channel string, socket socket.Socket) *answer.Error
	Unsubscribe(channel string, socket socket.Socket) *answer.Error
	UnsubscribeFromAll(socketID string)
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

// ONLY FOR TEST PURPOSE
// SetState is only here to be used to reset the state before each test
func SetState(subs Subscriber, peers Peerer, queries Querier) {
	instance = &state{
		subs:    subs,
		peers:   peers,
		queries: queries,
	}
}

func getSubs() (Subscriber, *answer.Error) {
	if instance == nil || instance.subs == nil {
		return nil, answer.NewInternalServerError("subscriber was not instantiated")
	}

	return instance.subs, nil
}

func AddChannel(channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.AddChannel(channel)
}

func HasChannel(channel string) (bool, *answer.Error) {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return false, errAnswer
	}

	return subs.HasChannel(channel), nil
}

func Subscribe(socket socket.Socket, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.Subscribe(channel, socket)
}

func Unsubscribe(socket socket.Socket, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.Unsubscribe(channel, socket)
}

func UnsubscribeFromAll(socketID string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	subs.UnsubscribeFromAll(socketID)

	return nil
}

func SendToAll(buf []byte, channel string) *answer.Error {
	subs, errAnswer := getSubs()
	if errAnswer != nil {
		return errAnswer
	}

	return subs.SendToAll(buf, channel)
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

func getQueries() (Querier, *answer.Error) {
	if instance == nil || instance.queries == nil {
		return nil, answer.NewInternalServerError("querier was not instantiated")
	}

	return instance.queries, nil
}

func GetNextID() (int, *answer.Error) {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return -1, errAnswer
	}

	return queries.GetNextID(), nil
}

func SetQueryReceived(ID int) *answer.Error {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return errAnswer
	}

	err := queries.SetQueryReceived(ID)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("%v", err)
		return errAnswer
	}

	return nil
}

func AddQuery(ID int, query method.GetMessagesById) *answer.Error {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return errAnswer
	}

	queries.AddQuery(ID, query)

	return nil
}
