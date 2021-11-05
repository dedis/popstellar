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

func TestBaseChannel_RollCallOrder(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}

	// Create the channel
	cha := NewChannel("/root/blabla", fakeHub, messages[0], nolog)

	laoChannel, ok := cha.(*Channel)
	require.True(t, ok)

	_, found := fakeHub.channelByID["/root/blabla/social/chirps/"]
	require.True(t, found)

	time.Sleep(time.Millisecond)

	for i := 1; i < numMessages; i++ {
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
	catchupAnswer := cha.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

func TestBaseChannel_ConsensusIsCreated(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 1

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}

	// Create the channel
	channel := NewChannel("channel0", fakeHub, messages[0], nolog)

	_, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	consensusID := "channel0/consensus"
	consensus := fakeHub.channelByID[consensusID]
	require.NotNil(t, consensus)
}

func TestBaseChannel_GeneralChirpingIsCreated(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 1

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}

	// Create the channel
	channel := NewChannel("/root/channel0", fakeHub, messages[0], nolog)

	_, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	_, found := fakeHub.channelByID["/root/channel0/social/chirps/"]
	require.True(t, found)

}

func TestBaseChannel_CreationChirpChannel(t *testing.T) {
	publicKey := "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="

	pk, err := base64.URLEncoding.DecodeString(publicKey)
	require.NoError(t, err)
	point := crypto.Suite.Point()
	err = point.UnmarshalBinary(pk)
	require.NoError(t, err)

	// Create the hub
	fakeHub, err := NewfakeHub(point, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 1

	messages := make([]message.Message, numMessages)

	messages[0] = message.Message{MessageID: "0"}

	// Create the channel
	channel := NewChannel("channel0", fakeHub, messages[0], nolog)

	_, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	newDataCreate := messagedata.RollCallCreate{
		Object: "roll_call",
		Action: "create",
		ID: messagedata.Hash("R", "channel0", "1633098853", "Roll Call"),
		Name: "Roll Call",
		Creation: 1633098853,
		ProposedStart: 1633099125,
		ProposedEnd: 1633099140,
		Location: "EPFL",
		Description: "Food is welcome!",
	}

	dataBufCreate, err := json.Marshal(newDataCreate)
	require.Nil(t, err)

	newData64Create := base64.URLEncoding.EncodeToString(dataBufCreate)

	rpcMessageCreate := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		ID: 1,
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			"channel0",
			message.Message{
				Data: newData64Create,
				Sender: "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
				Signature: "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw==",
				MessageID: messagedata.Hash(newData64Create, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw=="),
				WitnessSignatures: []message.WitnessSignature{},
			},
		},
	}
	
	err = channel.Publish(rpcMessageCreate)
	require.Nil(t, err)

	newDataOpen := messagedata.RollCallOpen{
		Object: "roll_call",
		Action: "open",
		UpdateID: messagedata.Hash("R", "channel0", messagedata.Hash("R", "channel0", "1633098853", "Roll Call"), "1633099127"),
		Opens: messagedata.Hash("R", "channel0", "1633098853", "Roll Call"),
		OpenedAt: 1633099127,
	}

	dataBufOpen, err := json.Marshal(newDataOpen)
	require.Nil(t, err)

	newData64Open := base64.URLEncoding.EncodeToString(dataBufOpen)

	rpcMessageOpen := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		ID: 2,
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			"channel0",
			message.Message{
				Data: newData64Open,
				Sender: "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
				Signature: "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw==",
				MessageID: messagedata.Hash(newData64Open, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw=="),
				WitnessSignatures: []message.WitnessSignature{},
			},
		},
	}

	err = channel.Publish(rpcMessageOpen)
	require.Nil(t, err)

	newDataClose := messagedata.RollCallClose{
		Object: "roll_call",
		Action: "close",
		UpdateID: messagedata.Hash("R", "channel0", messagedata.Hash("R", "channel0", messagedata.Hash("R", "channel0", "1633098853", "Roll Call"), "1633099127"), "1633099135"),
		Closes: messagedata.Hash("R", "channel0", messagedata.Hash("R", "channel0", "1633098853", "Roll Call"), "1633099127"),
		ClosedAt: 1633099135,
		Attendees: []string{"M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="},
	}

	dataBufClose, err := json.Marshal(newDataClose)
	require.Nil(t, err)

	newData64Close := base64.URLEncoding.EncodeToString(dataBufClose)

	rpcMessageClose := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		ID: 3,
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			"channel0",
			message.Message{
				Data: newData64Close,
				Sender: "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
				Signature: "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw==",
				MessageID: messagedata.Hash(newData64Close, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw=="),
				WitnessSignatures: []message.WitnessSignature{},
			},
		},
	}
	err = channel.Publish(rpcMessageClose)
	require.Nil(t, err)

	time.Sleep(time.Millisecond)

	_, found := fakeHub.channelByID["channel0/social/" + "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=" + "/"]
	require.True(t, found)
}

func Test_Verify_Functions(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	numMessages := 1

	messages := make([]message.Message, numMessages)

	channel := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, messages[0], nolog)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Get the JSON
	relativeExamplePath := filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData")
	file := filepath.Join(relativeExamplePath, "roll_call_open.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "roll_call", object)
	require.Equal(t, "open", action)

	var msg messagedata.RollCallOpen

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	// Test the function
	err = laoChannel.verifyMessageRollCallOpenID(msg)
	require.NoError(t, err)
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

// NewfakeHub returns a fake Organizer Hub.
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
	h.Unlock()
}

func (h *fakeHub) GetPubkey() kyber.Point {
	return h.public
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}
