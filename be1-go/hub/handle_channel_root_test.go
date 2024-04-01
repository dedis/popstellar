package hub

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"io"
	"popstellar/crypto"
	"popstellar/hub/mocks"
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"testing"
	"time"
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
		socket:              fakeSocket{id: "fakeID"},
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

func Test_MockExample(t *testing.T) {
	repo := mocks.NewRepository(t)
	repo.On("GetMessageByID", "messageID1").Return(message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}, nil)
	msg, err := repo.GetMessageByID("messageID1")
	if err != nil {
		return
	}
	log.Info().Msg(msg.MessageID)
}

func Test_handleChannelRoot(t *testing.T) {
	mockRepository := mocks.NewRepository(t)
	params := newHandlerParameters(mockRepository)
	keypair := generateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"

	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
}
