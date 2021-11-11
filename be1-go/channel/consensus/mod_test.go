package consensus

import (
	"encoding/json"
	"fmt"
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

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const protocolRelativePath string = "../../../protocol"

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
	point := suite.Point().Pick(suite.RandomStream())
	point = point.Mul(secret, point)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}

type fakeHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]channel.Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac channel.LaoFactory
}

// NewHub returns a Organizer Hub.
func NewfakeHub(public kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*fakeHub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	hub := fakeHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		public:          public,
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
	fmt.Printf("cccc")
	h.Unlock()
}

func (h *fakeHub) GetPubkey() kyber.Point {
	return h.public
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
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
