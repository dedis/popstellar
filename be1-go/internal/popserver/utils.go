package popserver

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	"io"
	"popstellar/crypto"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
)

// FakeSocket is a fake implementation of a Socket
//
// - implements socket.Socket
type FakeSocket struct {
	socket.Socket

	ResultID    int
	Res         []message.Message
	MissingMsgs map[string][]message.Message
	Msg         []byte

	Err error

	// the Socket ID
	Id string
}

// Send implements socket.Socket
func (f *FakeSocket) Send(msg []byte) {
	f.Msg = msg
}

// SendResult implements socket.Socket
func (f *FakeSocket) SendResult(id int, res []message.Message, missingMsgs map[string][]message.Message) {
	f.ResultID = id
	f.Res = res
	f.MissingMsgs = missingMsgs
}

// SendError implements socket.Socket
func (f *FakeSocket) SendError(id *int, err error) {
	f.Err = err
}

func (f *FakeSocket) ID() string {
	return f.Id
}

func (f *FakeSocket) GetMessage() []byte {
	return f.Msg
}

func (f *FakeSocket) Type() socket.SocketType {
	return socket.ClientSocketType
}

func NewHandlerParameters(db repo.Repository) types.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()

	return types.HandlerParameters{
		Log:                 nolog,
		Socket:              &FakeSocket{Id: "fakeID"},
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		OwnerPubKey:         nil,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
		ServerPubKey:        nil,
		ServerSecretKey:     nil,
	}
}

func NewHandlerParametersWithOwnerAndServer(db repo.Repository, owner kyber.Point, server Keypair) types.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()

	return types.HandlerParameters{
		Log:                 nolog,
		Socket:              &FakeSocket{Id: "fakeID"},
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		OwnerPubKey:         owner,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
		ServerPubKey:        server.Public,
		ServerSecretKey:     server.Private,
	}
}

func NewHandlerParametersWithFakeSocket(db repo.Repository, s *FakeSocket) types.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()

	return types.HandlerParameters{
		Log:                 nolog,
		Socket:              s,
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		OwnerPubKey:         nil,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
	}

}

type Keypair struct {
	Public     kyber.Point
	PublicBuf  []byte
	Private    kyber.Scalar
	PrivateBuf []byte
}

func GenerateKeyPair(t *testing.T) Keypair {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	publicBuf, err := point.MarshalBinary()
	require.NoError(t, err)
	privateBuf, err := secret.MarshalBinary()

	return Keypair{point, publicBuf, secret, privateBuf}
}

type FakeSubscribers struct {
	sync.RWMutex
	list map[string]map[string]socket.Socket
}

func NewFakeSubscribers() *FakeSubscribers {
	return &FakeSubscribers{list: make(map[string]map[string]socket.Socket)}
}

func (s *FakeSubscribers) AddChannel(channel string) {
	s.Lock()
	defer s.Unlock()

	s.list[channel] = make(map[string]socket.Socket)
}

func (s *FakeSubscribers) Subscribe(channel string, socket socket.Socket) *answer.Error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Subscribe to unknown channel")
	}

	s.list[channel][socket.ID()] = socket

	return nil
}

func (s *FakeSubscribers) Unsubscribe(channel string, socket socket.Socket) *answer.Error {
	s.Lock()
	defer s.Unlock()

	_, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot Unsubscribe from unknown channel")
	}

	_, ok = s.list[channel][socket.ID()]
	if !ok {
		return answer.NewInvalidActionError("cannot Unsubscribe from a channel not subscribed")
	}

	delete(s.list[channel], socket.ID())

	return nil
}

// SendToAll sends a message to all sockets.
func (s *FakeSubscribers) SendToAll(buf []byte, channel string) *answer.Error {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channel]
	if !ok {
		return answer.NewInvalidResourceError("failed to send to all clients, channel %s not found", channel)
	}
	for _, v := range sockets {
		v.Send(buf)
	}

	return nil
}

func (s *FakeSubscribers) HasChannel(channel string) bool {
	s.RLock()
	defer s.RUnlock()

	_, ok := s.list[channel]
	if !ok {
		return false
	}

	return true
}

func (s *FakeSubscribers) IsSubscribed(channel string, socket socket.Socket) (bool, error) {
	s.RLock()
	defer s.RUnlock()

	sockets, ok := s.list[channel]
	if !ok {
		return false, xerrors.Errorf("channel doesn't exist")
	}
	_, ok = sockets[socket.ID()]
	if !ok {
		return false, nil
	}

	return true, nil
}
