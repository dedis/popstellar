package coin

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
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
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const protocolRelativePath string = "../../../protocol"

// Tests that the channel works correctly when it receives a subscribe
func Test_General_Channel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)

	channelDC, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "subscribe", "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}

	err = channelDC.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, channelDC.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe
func Test_General_Channel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)

	channelDC, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket"}
	channelDC.sockets.Upsert(socket)

	err = channelDC.Unsubscribe("socket", message)
	require.NoError(t, err)

	require.False(t, channelDC.sockets.Delete("socket"))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func Test_General_Channel_Wrong_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)

	channelDC, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = channelDC.Unsubscribe("socket", message)
	require.Error(t, err, "client is not subscribed to this channel")
}

// Tests that the channel works correctly when it receives a catchup
func Test_Coin_Channel_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5
	messages := make([]message.Message, numMessages)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)

	channelDC, ok := channel.(*Channel)
	require.True(t, ok)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		channelDC.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := channelDC.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// Tests that the channel throws an error when it receives a publish message
func Test_General_Channel_Publish(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)

	channelDC, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Publish
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = channelDC.Publish(message, socket.ClientSocket{})
	require.Error(t, err, "nothing should be directly published in the general")
}

// Tests that the channel works correctly when it receives a transaction
func Test_SendTransaction(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction.json")
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
	message.Params.Channel = digitalCashChannelName

	require.NoError(t, channel.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel works correctly when it receives a large transaction
func Test_SendTransactionMaxAmount(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_max_amount.json")
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
	message.Params.Channel = digitalCashChannelName

	require.NoError(t, channel.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel rejects transactions that exceed the maximum amount
func Test_SendTransactionOverflowAmount(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_overflow_amount.json")
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
	message.Params.Channel = digitalCashChannelName

	require.Error(t, channel.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel accepts transactions with zero amounts
func Test_SendTransactionZeroAmount(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_zero_amount.json")
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
	message.Params.Channel = digitalCashChannelName

	require.NoError(t, channel.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel rejects transactions with negative amounts
func Test_SendTransactionNegativeAmount(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_negative_amount.json")
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
	message.Params.Channel = digitalCashChannelName

	require.Error(t, channel.Publish(message, socket.ClientSocket{}))
}

// Tests that the channel throw an error when receiving an incomplete json message
func Test_SendTransaction_MissingData(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// load example
	require.NoError(t, err)

	m := message.Message{
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash("helloworld", "h"),
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
	message.Params.Channel = digitalCashChannelName

	err = channel.Publish(message, socket.ClientSocket{})
	require.EqualError(t, err, "failed to verify publish message: failed to "+
		"verify json schema: failed to validate schema: EOF")
}

// Tests that the channel works correctly when it receives a Transaction with wrong id
func Test_SendTransactionWrongId(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_wrong_transaction_id.json")
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
	message.Params.Channel = digitalCashChannelName

	// Transaction Id is not valid, value=0xBADID3AN0N0N0bvJC2LcZbm0chV1GrJDGfMlJSLRc=, computed=_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=
	err = channel.Publish(message, socket.ClientSocket{})
	require.EqualError(t, err, "failed to handle a publish message:"+
		" failed to process message:"+
		" failed to process action 'coin#post_transaction':"+
		" invalid coin#postTransaction message:"+
		" transaction id is not valid:"+
		" 0xBADID3AN0N0N0bvJC2LcZbm0chV1GrJDGfMlJSLRc= !="+
		" _6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=")
}

/*
// Tests that the channel works correctly when it receives a transaction
func Test_SendTransactionMultInpAndOut(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_multipleinpandout.json")
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
	message.Params.Channel = digitalCashChannelName

	require.NoError(t, channel.Publish(message, socket.ClientSocket{}))
}
*/

// Tests that the channel works correctly when it receives a transaction
func Test_SendTransactionCoinbase(t *testing.T) {
	// Create the hub

	var laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var digitalCashChannelName = "/root/" + laoID + "/coin"

	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel(digitalCashChannelName, fakeHub, nolog)

	fakeHub.RegisterNewChannel(digitalCashChannelName, channel)
	_, found := fakeHub.channelByID[digitalCashChannelName]
	require.True(t, found)

	// Create the message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	// load example
	file := filepath.Join(relativePath, "coin", "post_transaction_coinbase.json")
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
	message.Params.Channel = digitalCashChannelName

	require.NoError(t, channel.Publish(message, socket.ClientSocket{}))
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

func (h *fakeHub) RegisterNewChannel(channeID string, channel channel.Channel) {
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
