package chirp

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/channel/generalChirping"
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
	laoID                             = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	sender                            = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	generalName                       = "/root/" + laoID + "/social/posts"
	chirpChannelName                  = "/root/" + laoID + "/social/" + sender
	relativeMsgDataExamplePath string = "../../../protocol/examples/messageData"
	relativeQueryExamplePath   string = "../../../protocol/examples/query"
)

// Tests that the channel works correctly when it receives a subscribe from a
// client
func Test_Chirp_Channel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	cha := NewChannel(chirpChannelName, sender, fakeHub, nil, nolog)

	file := filepath.Join(relativeQueryExamplePath, "subscribe", "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg method.Subscribe
	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	fakeSock := &fakeSocket{id: "socket", sockType: socket.ClientSocketType}

	err = cha.Subscribe(fakeSock, msg)
	require.NoError(t, err)

	// Delete returns false if the socket is not present in the store
	require.True(t, cha.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe from a
// client
func Test_Chirp_Channel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	cha := NewChannel(chirpChannelName, sender, fakeHub, nil, nolog)

	file := filepath.Join(relativeQueryExamplePath, "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg method.Unsubscribe
	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	fakeSock := &fakeSocket{id: "socket", sockType: socket.ClientSocketType}
	cha.sockets.Upsert(fakeSock)

	err = cha.Unsubscribe("socket", msg)
	require.NoError(t, err)

	// Delete returns false if the socket is not present in the store
	require.False(t, cha.sockets.Delete("socket"))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func Test_Chirp_Channel_Wrong_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	cha := NewChannel(chirpChannelName, sender, fakeHub, nil, nolog)

	file := filepath.Join(relativeQueryExamplePath, "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg method.Unsubscribe
	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// Delete returns false if the socket is not present in the store
	require.Error(t, cha.Unsubscribe("socket", msg))
}

// Tests that the channel works correctly when it receives a catchup
func Test_Chirp_Channel_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channels
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)

	cha := NewChannel(chirpChannelName, sender, fakeHub, generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, cha)

	_, found := fakeHub.channelByID[chirpChannelName]
	require.True(t, found)
	_, found = fakeHub.channelByID[generalName]
	require.True(t, found)

	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		msg := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = msg

		// Store the message in the inbox
		cha.inbox.StoreMessage(msg)

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

// Tests that the channel works when it receives a broadcast message
func Test_Chirp_Channel_Broadcast(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channels
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)
	cha := NewChannel(chirpChannelName, sender, fakeHub, generalCha, nolog)

	fakeSock := &fakeSocket{id: "fakeSock"}
	cha.sockets.Upsert(fakeSock)

	// Create the message
	file := filepath.Join(relativeMsgDataExamplePath, "chirp_add_publish",
		"chirp_add_publish.json")
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

	file = filepath.Join(relativeQueryExamplePath, "broadcast", "broadcast.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var msg method.Broadcast
	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	msg.Base = query.Base{
		JSONRPCBase: jsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		Method: "broadcast",
	}
	msg.Params.Channel = cha.channelID
	msg.Params.Message = m

	require.NoError(t, cha.Broadcast(msg, nil))

	broadBuf, err := json.Marshal(msg)
	require.NoError(t, err)

	require.Equal(t, broadBuf, fakeSock.msg)
}

// Tests that the channel works correctly when receiving an add chirp message
func Test_Send_Chirp(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channels
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)
	cha := NewChannel(chirpChannelName, sender, fakeHub, generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, cha)
	_, found := fakeHub.channelByID[chirpChannelName]
	require.True(t, found)
	_, found = fakeHub.channelByID[generalName]
	require.True(t, found)

	time.Sleep(time.Millisecond)

	// Create the message
	file := filepath.Join(relativeMsgDataExamplePath, "chirp_add_publish",
		"chirp_add_publish.json")
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

	relativePathCreatePub := filepath.Join(relativeQueryExamplePath, "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var msg method.Publish

	err = json.Unmarshal(bufCreatePub, &msg)
	require.NoError(t, err)

	msg.Params.Message = m
	msg.Params.Channel = chirpChannelName

	require.NoError(t, cha.Publish(msg, socket.ClientSocket{}))

	msg2 := generalCha.Catchup(method.Catchup{ID: 0})

	checkData := messagedata.ChirpNotifyAdd{
		Object:    "chirp",
		Action:    "notify_add",
		ChirpID:   messagedata.Hash(buf64, "h"),
		Channel:   generalName,
		Timestamp: 1634760180,
	}

	checkDataBuf, err := json.Marshal(checkData)
	require.Nil(t, err)
	checkData64 := base64.URLEncoding.EncodeToString(checkDataBuf)

	// check if the data on the general is the same as the one we sent
	require.Equal(t, checkData64, msg2[0].Data)
}

// Tests that the channel works correctly when receiving a delete chirp message
func Test_Delete_Chirp(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channels
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)
	cha := NewChannel(chirpChannelName, sender, fakeHub, generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, cha)
	_, found := fakeHub.channelByID[chirpChannelName]
	require.True(t, found)
	_, found = fakeHub.channelByID[generalName]
	require.True(t, found)

	time.Sleep(time.Millisecond)

	// Create the add chirp message
	file := filepath.Join(relativeMsgDataExamplePath, "chirp_add_publish",
		"chirp_add_publish.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64add := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64add,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64add, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	addChirpID := m.MessageID

	relativePathCreatePub := filepath.Join(relativeQueryExamplePath, "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var pub method.Publish

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = chirpChannelName

	// publish add chirp message
	require.NoError(t, cha.Publish(pub, socket.ClientSocket{}))

	time.Sleep(time.Millisecond)

	// create delete chirp message
	file = filepath.Join(relativeMsgDataExamplePath, "chirp_delete_publish",
		"chirp_delete_publish.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var chirpDel messagedata.ChirpDelete

	err = json.Unmarshal(buf, &chirpDel)
	require.NoError(t, err)

	chirpDel.ChirpID = addChirpID

	buf, err = json.Marshal(chirpDel)
	require.NoError(t, err)

	buf64delete := base64.URLEncoding.EncodeToString(buf)

	m = message.Message{
		Data:              buf64delete,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64delete, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	pub.Params.Message = m
	pub.Params.Channel = chirpChannelName

	// publish delete chirp message
	require.NoError(t, cha.Publish(pub, socket.ClientSocket{}))

	msg := generalCha.Catchup(method.Catchup{ID: 0})

	checkDataAdd := messagedata.ChirpNotifyAdd{
		Object:    "chirp",
		Action:    "notify_add",
		ChirpID:   messagedata.Hash(buf64add, "h"),
		Channel:   generalName,
		Timestamp: 1634760180,
	}
	checkDataBufAdd, err := json.Marshal(checkDataAdd)
	require.Nil(t, err)
	checkData64Add := base64.URLEncoding.EncodeToString(checkDataBufAdd)

	checkDataDelete := messagedata.ChirpNotifyDelete{
		Object:    "chirp",
		Action:    "notify_delete",
		ChirpID:   messagedata.Hash(buf64delete, "h"),
		Channel:   generalName,
		Timestamp: 1634760180,
	}
	checkDataBufDelete, err := json.Marshal(checkDataDelete)
	require.Nil(t, err)
	checkData64Delete := base64.URLEncoding.EncodeToString(checkDataBufDelete)

	// check if the data on the general is the same as the one we sent
	require.Equal(t, checkData64Add, msg[0].Data)
	require.Equal(t, checkData64Delete, msg[1].Data)
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

	sockType socket.SocketType

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the socket ID
	id string
}

// Get the type of the fake socket
func (f *fakeSocket) Type() socket.SocketType {
	return f.sockType
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
