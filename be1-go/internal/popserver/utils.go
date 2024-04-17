package popserver

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"io"
	"popstellar/crypto"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/types"
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

func NewHandlerParameters(db repo.Repository) types.HandlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return types.HandlerParameters{
		Log:                 nolog,
		Socket:              &FakeSocket{Id: "fakeID"},
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		OwnerPubKey:         nil,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
		ServerPubKey:        point,
		ServerSecretKey:     secret,
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
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	return types.HandlerParameters{
		Log:                 nolog,
		Socket:              s,
		SchemaValidator:     *schemaValidator,
		DB:                  db,
		OwnerPubKey:         nil,
		ClientServerAddress: "ClientServerAddress",
		ServerServerAddress: "ServerServerAddress",
		ServerPubKey:        point,
		ServerSecretKey:     secret,
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
