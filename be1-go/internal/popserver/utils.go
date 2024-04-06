package popserver

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"io"
	"popstellar/crypto"
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/internal/popserver/repo"
	state2 "popstellar/internal/popserver/state"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
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

func NewHandlerParameters(db repo.Repository) state2.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	peers := state.NewPeers()
	queries := state.NewQueries(nolog)

	return state2.HandlerParameters{
		Log:                 nolog,
		Socket:              &FakeSocket{Id: "fakeID"},
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		Subs:                make(state2.Subscribers),
		Peers:               &peers,
		Queries:             &queries,
		OwnerPubKey:         nil,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
	}
}

func NewHandlerParametersWithFakeSocket(db repo.Repository, s *FakeSocket) state2.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	peers := state.NewPeers()
	queries := state.NewQueries(nolog)

	return state2.HandlerParameters{
		Log:                 nolog,
		Socket:              s,
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		Subs:                make(state2.Subscribers),
		Peers:               &peers,
		Queries:             &queries,
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
