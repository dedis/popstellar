package lao

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"

	"github.com/stretchr/testify/require"
)

const protocolRelativePath string = "../../../protocol"

func TestLAOChannel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}

	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "subscribe")

	file := filepath.Join(relativePath, "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "sockSocket"}

	err = channel.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, laoChannel.sockets.Delete("sockSocket"))
}

func TestLAOChannel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "sockSocket"}
	laoChannel.sockets.Upsert(socket)

	require.NoError(t, channel.Unsubscribe("sockSocket", message))

	// we check that the sockSocket has been deleted
	require.False(t, laoChannel.sockets.Delete("sockSocket"))

	// unsubscribing two times with the same sockSocket must fail
	require.Error(t, channel.Unsubscribe("sockSocket", message))
}

func TestLAOChannel_wrongUnsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	// Should fail as it is not subscribed
	require.Error(t, channel.Unsubscribe("inexistingSocket", message))
}

// Tests that the channel works when it receives a broadcast message
func TestLAOChannel_Broadcast(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)
	laoChannel := channel.(*Channel)

	// Creates a sockSocket subscribed to the channel
	fakeSock := &fakeSocket{id: "sockSocket"}
	laoChannel.sockets.Upsert(fakeSock)

	// Create an update lao message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_update")

	file := filepath.Join(relativePath, "lao_update.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePath = filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast")

	file = filepath.Join(relativePath, "broadcast.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var message method.Broadcast
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	message.Base = query.Base{
		JSONRPCBase: jsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},

		Method: "broadcast",
	}
	message.Params.Channel = laoChannel.channelID
	message.Params.Message = m1

	require.NoError(t, channel.Broadcast(message, nil))

	// Check that the message is broadcast to the subscribed sockets
	bufBroad, err := json.Marshal(message)
	require.NoError(t, err)

	require.Equal(t, bufBroad, fakeSock.msg)
}

func TestLAOChannel_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages+1)

	messages[0] = message.Message{MessageID: "0"}
	messages[1] = message.Message{MessageID: "1"}

	// Create the channel
	channel, err := NewChannel("channel0", fakeHub, messages[0], nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	_, ok = laoChannel.inbox.GetMessage(messages[0].MessageID)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	for i := 2; i < numMessages+1; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		laoChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Change the greeting message id to make it easier to check
	catchupAnswer[1] = message.Message{MessageID: "1"}

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages+1; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

func TestLAOChannel_Publish_LaoUpdate(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	// Create an update lao message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_update")

	file := filepath.Join(relativePath, "lao_update.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathPub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	filePublish := filepath.Join(relativePathPub, "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = m1

	require.NoError(t, channel.Publish(messagePublish, nil))
}

func TestLAOChannel_Publish_LaoState(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)
	laoChannel := channel.(*Channel)

	// Create an update lao
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_update")

	file := filepath.Join(relativePath, "lao_update.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	// Store the update lao in the inbox
	laoChannel.inbox.StoreMessage(m1)

	// Create a lao_state message
	relativePathState := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_state")

	fileState := filepath.Join(relativePathState, "lao_state.json")
	bufState, err := os.ReadFile(fileState)
	require.NoError(t, err)

	var mState messagedata.LaoState
	err = json.Unmarshal(bufState, &mState)
	require.NoError(t, err)

	mState.ModificationID = messagedata.Hash(bufb64, publicKey64)
	mState.ModificationSignatures = []messagedata.ModificationSignature{}

	mStateBuf, err := json.Marshal(mState)
	require.NoError(t, err)

	bufState64 := base64.URLEncoding.EncodeToString(mStateBuf)

	m2 := message.Message{
		Data:              bufState64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufState64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathStatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileStatePublish := filepath.Join(relativePathStatePub, "publish.json")
	bufStatePub, err := os.ReadFile(fileStatePublish)
	require.NoError(t, err)

	var messageStatePublish method.Publish

	err = json.Unmarshal(bufStatePub, &messageStatePublish)
	require.NoError(t, err)

	messageStatePublish.Params.Message = m2

	require.NoError(t, channel.Publish(messageStatePublish, nil))
}

func TestBaseChannel_ConsensusIsCreated(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}

	// Create the channel
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	_, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	consensusID := "channel0/consensus"
	consensus := fakeHub.channelByID[consensusID]
	require.NotNil(t, consensus)
}

func TestBaseChannel_SimulateRollCall(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	// Create the hub
	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}

	// Create the channel
	channel, err := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	_, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	// Create the roll_call_create message
	relativePathCreate := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	fileCreate := filepath.Join(relativePathCreate, "roll_call_create.json")
	bufCreate, err := os.ReadFile(fileCreate)
	require.NoError(t, err)

	bufCreate64 := base64.URLEncoding.EncodeToString(bufCreate)

	m1 := message.Message{
		Data:              bufCreate64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufCreate64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var messageCreatePub method.Publish

	err = json.Unmarshal(bufCreatePub, &messageCreatePub)
	require.NoError(t, err)

	messageCreatePub.Params.Message = m1

	require.NoError(t, channel.Publish(messageCreatePub, nil))

	// Create the roll_call_open message
	relativePathOpen := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	fileOpen := filepath.Join(relativePathOpen, "roll_call_open.json")
	bufOpen, err := os.ReadFile(fileOpen)
	require.NoError(t, err)

	bufOpen64 := base64.URLEncoding.EncodeToString(bufOpen)

	m2 := message.Message{
		Data:              bufOpen64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufOpen64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	messageOpenPub := messageCreatePub

	messageOpenPub.Params.Message = m2

	require.NoError(t, channel.Publish(messageOpenPub, nil))

	// Create the roll_call_close message
	relativePathClose := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	fileClose := filepath.Join(relativePathClose, "roll_call_close.json")
	bufClose, err := os.ReadFile(fileClose)
	require.NoError(t, err)

	bufClose64 := base64.URLEncoding.EncodeToString(bufClose)

	m3 := message.Message{
		Data:              bufClose64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufClose64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	messageClosePub := messageCreatePub
	messageClosePub.Params.Message = m3

	require.NoError(t, channel.Publish(messageClosePub, nil))
}

func TestLAOChannel_Election_Creation(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	// Create an update lao message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "election_setup", "election_setup.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathPub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	filePublish := filepath.Join(relativePathPub, "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = m1

	require.NoError(t, channel.Publish(messagePublish, nil))
}

func TestLAOChannel_Sends_Greeting(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	m := message.Message{MessageID: "0"}
	channel, err := NewChannel("/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})
	// should contain the creation message and the LAO greet
	require.Len(t, catchupAnswer, 2)

	greetMsg := catchupAnswer[1]

	var laoGreet messagedata.LaoGreet

	err = greetMsg.UnmarshalData(&laoGreet)
	require.NoError(t, err)

	require.Equal(t, messagedata.LAOObject, laoGreet.Object)
	require.Equal(t, messagedata.LAOActionGreet, laoGreet.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", laoGreet.LaoID)
	require.Equal(t, publicKey64, laoGreet.Frontend)
}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)
var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}

type fakeHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]channel.Channel

	closedSockets chan string

	pubKeyOwner kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac channel.LaoFactory
}

// NewHub returns a Organizer Hub.
func NewfakeHub(publicOrg kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*fakeHub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := fakeHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		pubKeyOwner:     publicOrg,
		pubKeyServ:      pubServ,
		secKeyServ:      secServ,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(10),
		log:             log,
		laoFac:          laoFac,
	}

	return &hub, nil
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

func (h *fakeHub) NotifyNewChannel(channeID string, channel channel.Channel, socket socket.Socket) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// GetPubKeyOwner implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyOwner() kyber.Point {
	return h.pubKeyOwner
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
}

// GetServerAddress implements channel.HubFunctionalities
func (h *fakeHub) GetServerAddress() string {
	return ""
}

// Sign implements channel.HubFunctionalities
func (h *fakeHub) Sign(data []byte) ([]byte, error) {
	return nil, nil
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

func (h *fakeHub) GetServerNumber() int {
	return 0
}

func (h *fakeHub) SendAndHandleMessage(msg method.Broadcast) error {
	return nil
}

// fakeSocket is a fake implementation of a Socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the sockSocket ID
	id string
}

// Send implements socket.Socket
func (f *fakeSocket) Send(msg []byte) {
	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message) {
	f.resultID = id
	f.res = res
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	if id != nil {
		f.resultID = *id
	} else {
		f.resultID = -1
	}
	f.err = err
}

func (f *fakeSocket) ID() string {
	return f.id
}
