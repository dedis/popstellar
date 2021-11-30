package consensus

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const protocolRelativePath string = "../../../protocol"

// Tests that the channel works correctly when it receives a subscribe
func Test_Consensus_Channel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "subscribe", "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}

	err = channel.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, consensusChannel.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe
func Test_Consensus_Channel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}
	consensusChannel.sockets.Upsert(socket)

	err = channel.Unsubscribe("socket", message)
	require.NoError(t, err)

	require.False(t, consensusChannel.sockets.Delete("socket"))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func Test_Consensus_Channel_Wrong_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = channel.Unsubscribe("socket", message)
	require.Error(t, err, "client is not subscribed to this channel")
}

// Tests that the channel works correctly when it receives a catchup
func Test_Consensus_Channel_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5
	messages := make([]message.Message, numMessages)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		consensusChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// Tests that the channel throws an error when it receives a broadcast message
func Test_Consensus_Channel_Broadcast(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Broadcast
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = consensusChannel.Broadcast(message)
	require.Error(t, err, "a consensus channel shouldn't need to broadcast a message")
}

// Tests that the channel works correctly when it receives an elect message
func Test_Consensus_Publish_Elect(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	consensusChanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a socket subscribed to the channel
	socket := &fakeSocket{id: "socket"}
	consensusChannel.sockets.Upsert(socket)

	// Create a consensus elect message
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	message := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = message

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = message
	messageBroadcast.Params.Channel = consensusChanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	err = channel.Publish(messagePublish, nil)
	require.NoError(t, err)
	require.Equal(t, byteBroad, socket.msg)
}

// Tests that the channel works correctly when it receives an elect-accept
// message
func Test_Consensus_Publish_Elect_Accept(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a socket subscribed to the channel
	socket := &fakeSocket{id: "socket"}
	consensusChannel.sockets.Upsert(socket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=",
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Create a consensus elect_accept message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect_accept", "elect_accept.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	message := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = message

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = message
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, socket.msg)
}

// Tests that the channel works correctly when it receives a learn message
func Test_Consensus_Publish_Elect_Learn(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a socket subscribed to the channel
	socket := &fakeSocket{id: "socket"}
	consensusChannel.sockets.Upsert(socket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=",
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Create a consensus learn message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_learn", "learn.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	message := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = message

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = message
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, socket.msg)
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

	pubKeyOrg kyber.Point

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
		pubKeyOrg:       publicOrg,
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

func (h *fakeHub) RegisterNewChannel(channeID string, channel channel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// GetPubKeyOrg implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyOrg() kyber.Point {
	return h.pubKeyOrg
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyServ() kyber.Point {
	return h.pubKeyOrg
}

// Sign implements channel.HubFunctionalities
func (h *fakeHub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

func (h *fakeHub) NotifyNewChannel(channelID string, channel channel.Channel, socket socket.Socket) {}

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the socket ID
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
