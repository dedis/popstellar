package reaction

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
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const (
	laoID                       = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	sender                      = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	reactionChannelName         = "/root/" + laoID + "/social/reactions"
	protocolRelativePath string = "../../../protocol"
)

// Tests that the channel works correctly when it receives a subscribe from a
// client
func TestReactionChannel_Subscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "subscribe")

	file := filepath.Join(relativePath, "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}

	err = cha.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, cha.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe from a
// client
func TestReactionChannel_Unsubscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}
	cha.sockets.Upsert(socket)

	require.NoError(t, cha.Unsubscribe("socket", message))

	// we check that the socket has been deleted
	require.False(t, cha.sockets.Delete("socket"))

	// unsubscribing two times with the same socket must fail
	require.Error(t, cha.Unsubscribe("socket", message))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func TestLAOChannel_wrongUnsubscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	// Should fail as it is not subscribed
	require.Error(t, cha.Unsubscribe("inexistingSocket", message))
}

// Tests that the channel works when it receives a broadcast message
func TestReactionChannel_Broadcast(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	cha.AddAttendee("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")

	fakeSock := &fakeSocket{id: "socket"}
	cha.sockets.Upsert(fakeSock)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "reaction_add", "reaction_add.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
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
	message.Params.Channel = cha.channelID
	message.Params.Message = m

	require.NoError(t, cha.Broadcast(message, nil))

	// Checks that the broadcast message is broadcast to the sockets
	bufBroad, err := json.Marshal(message)
	require.NoError(t, err)

	require.Equal(t, bufBroad, fakeSock.msg)
}

// Tests that the channel works correctly when it receives a catchup
func Test_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, cha)

	_, found := fakeHub.channelByID[reactionChannelName]
	require.True(t, found)

	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		cha.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := cha.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// Tests that the channel works correctly when it receives a reaction
func Test_SendReaction(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, cha)
	_, found := fakeHub.channelByID[reactionChannelName]
	require.True(t, found)

	cha.AddAttendee("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "reaction_add", "reaction_add.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var message method.Publish

	err = json.Unmarshal(bufCreatePub, &message)
	require.NoError(t, err)

	message.Params.Message = m
	message.Params.Channel = reactionChannelName

	require.NoError(t, cha.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel throws an error when it receives a delete reaction
// request on a reaction that has never been posted
func Test_DeleteAbsentReaction_MustFail(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, cha)
	_, found := fakeHub.channelByID[reactionChannelName]
	require.True(t, found)

	cha.AddAttendee("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")

	// Create delete reaction message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")
	file := filepath.Join(relativePath, "reaction_delete", "reaction_delete.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var pub method.Publish

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = reactionChannelName

	// If the reaction to delete does not exit, it must fail
	require.Error(t, cha.Publish(pub, socket.ClientSocket{}))
}

// Tests that the channel works correctly when it receives a delete reaction request
func Test_DeleteReaction(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, cha)
	_, found := fakeHub.channelByID[reactionChannelName]
	require.True(t, found)

	cha.AddAttendee("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")

	// First we send a reaction to be deleted
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "reaction_add", "reaction_add.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	addReactionID := m.MessageID

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var pub method.Publish

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = reactionChannelName

	// We publish the reaction to be deleted
	require.NoError(t, cha.Publish(pub, socket.ClientSocket{}))

	// Create delete reaction message
	file = filepath.Join(relativePath, "reaction_delete", "reaction_delete.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var del messagedata.ReactionDelete

	err = json.Unmarshal(buf, &del)
	require.NoError(t, err)

	// We set the reactionId with the ID obtain above
	del.ReactionID = addReactionID

	buf, err = json.Marshal(del)
	require.NoError(t, err)

	buf64 = base64.URLEncoding.EncodeToString(buf)

	m = message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	deleteReactionID := m.MessageID

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = reactionChannelName

	// Wait before storing a new message to be able to have a unique
	// timestamp for each message
	time.Sleep(time.Millisecond)

	// If there is no error, the delete request has been properly received
	require.NoError(t, cha.Publish(pub, socket.ClientSocket{}))

	// Check that the messages are stored in the inbox
	require.Equal(t, addReactionID, cha.inbox.GetSortedMessages()[0].MessageID)
	require.Equal(t, deleteReactionID, cha.inbox.GetSortedMessages()[1].MessageID)
}

// Tests that the channel works correctly when it receives a delete reaction request
// before a send reaction request.
func Test_DeleteReaction_Out_of_Order(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, cha)
	_, found := fakeHub.channelByID[reactionChannelName]
	require.True(t, found)

	cha.AddAttendee("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")

	// Sending the reaction to be deleted in a go routine, with some delay
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "reaction_add", "reaction_add.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	addReactionID := m.MessageID

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var pub method.Publish

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = reactionChannelName

	// Publishing the reaction with some delay in a go routine
	go func() {
		time.Sleep(50 * time.Millisecond)
		err := cha.Publish(pub, socket.ClientSocket{})
		require.NoError(t, err)
	}()

	// we create a new Publish variable for the delete message, as the previous
	// one has not yet been used for the add reaction
	var pub2 method.Publish
	// Create delete reaction message
	file = filepath.Join(relativePath, "reaction_delete", "reaction_delete.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var del messagedata.ReactionDelete

	err = json.Unmarshal(buf, &del)
	require.NoError(t, err)

	// We set the reactionId with the ID obtain above
	del.ReactionID = addReactionID

	buf, err = json.Marshal(del)
	require.NoError(t, err)

	buf64 = base64.URLEncoding.EncodeToString(buf)

	m = message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	deleteReactionID := m.MessageID

	err = json.Unmarshal(bufCreatePub, &pub2)
	require.NoError(t, err)

	pub2.Params.Message = m
	pub2.Params.Channel = reactionChannelName

	// Wait before storing a new message to be able to have a unique
	// timestamp for each message
	time.Sleep(10 * time.Millisecond)

	// If there is no error, the delete request has been properly received
	require.NoError(t, cha.Publish(pub2, socket.ClientSocket{}))

	// Check that the messages are stored in the inbox
	require.Equal(t, addReactionID, cha.inbox.GetSortedMessages()[0].MessageID)
	require.Equal(t, deleteReactionID, cha.inbox.GetSortedMessages()[1].MessageID)
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

// NewFakeHub returns a fake Hub.
func NewFakeHub(publicOrg kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*fakeHub, error) {

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

func (h *fakeHub) RegisterNewChannel(channeID string, channel channel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
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

// GetPeerAddresses implements channel.HubFunctionalities
func (h *fakeHub) GetPeerAddresses() []string {
	return nil
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

func (h *fakeHub) GetServerNumber() int {
	return 0
}

func (h *fakeHub) SendAndHandleMessage(msg method.Broadcast) error {
	return nil
}

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
func (f *fakeSocket) SendResult(id int, res []message.Message, missingMsgs map[string][]message.Message) {
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
