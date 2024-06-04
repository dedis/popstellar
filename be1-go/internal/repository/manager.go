package repository

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"sync"
)

type SocketManager interface {
	SendToAll(buf []byte)
	SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte)
	Upsert(socket socket.Socket)
	Delete(ID string) bool
}

type QueryManager interface {
	GetQueryState(ID int) (bool, error)
	GetNextID() int
	SetQueryReceived(ID int) error
	AddQuery(ID int, query method.GetMessagesById)
	AddRumorQuery(id int, query method.Rumor)
	IsRumorQuery(queryID int) bool
	GetRumorFromPastQuery(queryID int) (method.Rumor, bool)
}

type SubscriptionManager interface {
	AddChannel(channel string) *answer.Error
	HasChannel(channel string) bool
	Subscribe(channel string, socket socket.Socket) *answer.Error
	Unsubscribe(channel string, socket socket.Socket) *answer.Error
	UnsubscribeFromAll(socketID string)
	SendToAll(buf []byte, channel string) *answer.Error
}

type PeerManager interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	AddPeerGreeted(socketID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

type ConfigManager interface {
	GetOwnerPublicKeyInstance() (kyber.Point, *answer.Error)
	GetServerPublicKeyInstance() (kyber.Point, *answer.Error)
	GetServerSecretKeyInstance() (kyber.Scalar, *answer.Error)
	GetServerInfo() (string, string, string, *answer.Error)
}

type HubManager interface {
	GetWaitGroup() *sync.WaitGroup
	GetMessageChan() chan socket.IncomingMessage
	GetStopChan() chan struct{}
	GetClosedSockets() chan string
}
