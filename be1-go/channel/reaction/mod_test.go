package reaction

import (
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
	"time"
)

const (
	laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	root = "/root/"
	social = "/social/"
	reactions = "reactions"
	protocolRelativePath string = "../../../protocol"
)


func TestReactionChannel_Subscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	reactionChannelName := root + laoID + social + reactions

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

func TestReactionChannel_Unsubscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	reactionChannelName := root + laoID + social + reactions

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

func TestLAOChannel_wrongUnsubscribe(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	reactionChannelName := root + laoID + social + reactions

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

func TestReactionChannel_Broadcast_mustFail(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	reactionChannelName := root + laoID + social + reactions

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast")

	file := filepath.Join(relativePath, "broadcast.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Broadcast
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	// a lao channel isn't supposed to broadcast a message - must fail
	require.Error(t, cha.Broadcast(message))
}

func Test_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	reactionChannelName := root + laoID + social + reactions

	// Create the channel
	cha := NewChannel(reactionChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(reactionChannelName, &cha)

	_, found := fakeHub.channelByID[root+laoID+social+reactions]
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

func Test_SendReaction(t *testing.T) {
	//TODO
}

func Test_DeleteReaction(t *testing.T) {
	//TODO
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

// NewfakeHub returns a fake Organizer Hub.
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