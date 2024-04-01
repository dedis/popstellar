package hub

import (
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"io"
	"popstellar/crypto"
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"testing"
)

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID    int
	res         []message.Message
	missingMsgs map[string][]message.Message
	msg         []byte

	err error

	// the socket ID
	id string
}

// Send implements socket.Socket
func (f *fakeSocket) Send(msg []byte) {
	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message, missingMsgs map[string][]message.Message) {
	f.resultID = id
	f.res = res
	f.missingMsgs = missingMsgs
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	f.err = err
}

func (f *fakeSocket) ID() string {
	return f.id
}

func (f *fakeSocket) getMessage() []byte {
	return f.msg
}

func (f *fakeSocket) Type() socket.SocketType {
	return socket.ClientSocketType
}

func newHandlerParameters(db Repository) handlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	peers := state.NewPeers()
	queries := state.NewQueries(nolog)

	return handlerParameters{
		log:                 nolog,
		socket:              &fakeSocket{id: "fakeID"},
		schemaValidator:     *schemaValidator,
		db:                  db,
		subs:                make(subscribers),
		peers:               &peers,
		queries:             &queries,
		ownerPubKey:         nil,
		clientServerAddress: "clientServerAddress",
		serverServerAddress: "serverServerAddress",
	}
}

func newHandlerParametersWithFakeSocket(db Repository, s *fakeSocket) handlerParameters {
	nolog := zerolog.New(io.Discard)
	schemaValidator, _ := validation.NewSchemaValidator()
	peers := state.NewPeers()
	queries := state.NewQueries(nolog)

	return handlerParameters{
		log:                 nolog,
		socket:              s,
		schemaValidator:     *schemaValidator,
		db:                  db,
		subs:                make(subscribers),
		peers:               &peers,
		queries:             &queries,
		ownerPubKey:         nil,
		clientServerAddress: "clientServerAddress",
		serverServerAddress: "serverServerAddress",
	}

}

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

func generateKeyPair(t *testing.T) keypair {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}
