package repository

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
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
	AddChannel(channel string) error
	HasChannel(channel string) bool
	Subscribe(channel string, socket socket.Socket) error
	Unsubscribe(channel string, socket socket.Socket) error
	UnsubscribeFromAll(socketID string)
	SendToAll(buf []byte, channel string) error
	BroadcastToAllClients(msg message.Message, channel string) error
}

type PeerManager interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	AddPeerGreeted(socketID string)
	GetAllPeersInfo() []method.GreetServerParams
	IsPeerGreeted(socketID string) bool
}

type ConfigManager interface {
	GetOwnerPublicKey() kyber.Point
	GetServerPublicKey() kyber.Point
	GetServerSecretKey() kyber.Scalar
	GetServerInfo() (string, string, string, error)
	Sign(data []byte) ([]byte, error)
}

type HubManager interface {
	GetWaitGroup() *sync.WaitGroup
	GetMessageChan() chan socket.IncomingMessage
	GetStopChan() chan struct{}
	GetClosedSockets() chan string
}
