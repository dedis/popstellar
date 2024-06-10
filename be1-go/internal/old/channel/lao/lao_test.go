package lao

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"io"
	"os"
	"path/filepath"
	"popstellar/internal/crypto"
	jsonrpc "popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/message/messagedata/mlao"
	"popstellar/internal/message/method/mbroadcast"
	"popstellar/internal/message/method/mcatchup"
	"popstellar/internal/message/method/mgreetserver"
	"popstellar/internal/message/method/mpublish"
	"popstellar/internal/message/method/msubscribe"
	method2 "popstellar/internal/message/method/munsubscribe"
	"popstellar/internal/message/mmessage"
	"popstellar/internal/message/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/channel"
	"popstellar/internal/validation"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"

	"github.com/stretchr/testify/require"
)

const protocolRelativePath string = "../../../validation/protocol"

func TestLAOChannel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}

	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "subscribe")

	file := filepath.Join(relativePath, "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message msubscribe.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "sockSocket"}

	err = channel.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, laoChannel.sockets.Delete("sockSocket"))
}

func TestLAOChannel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method2.Unsubscribe
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

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel("channel0", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	relativePath := filepath.Join(protocolRelativePath,
		"examples", "query", "unsubscribe")

	file := filepath.Join(relativePath, "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method2.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	// Should fail as it is not subscribed
	require.Error(t, channel.Unsubscribe("inexistingSocket", message))
}

// Tests that the channel works when it receives a broadcast message
func TestLAOChannel_Broadcast(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
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

	m1 := mmessage.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufb64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePath = filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast")

	file = filepath.Join(relativePath, "broadcast.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var message mbroadcast.Broadcast
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	message.Base = mquery.Base{
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

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5

	messages := make([]mmessage.Message, numMessages+1)

	messages[0] = mmessage.Message{MessageID: "0"}
	messages[1] = mmessage.Message{MessageID: "1"}

	// Create the channel
	channel, err := NewChannel("channel0", fakeHub, messages[0], nolog, keypair.public, nil)
	require.NoError(t, err)

	laoChannel, ok := channel.(*Channel)
	require.True(t, ok)

	time.Sleep(time.Millisecond)

	for i := 2; i < numMessages+1; i++ {
		// Create a new message containing only an id
		message := mmessage.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		laoChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(mcatchup.Catchup{ID: 0})

	// Change the greeting message id to make it easier to check
	catchupAnswer[1] = mmessage.Message{MessageID: "1"}

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

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	// Create an update lao message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_update")

	file := filepath.Join(relativePath, "lao_update.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := mmessage.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufb64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePathPub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	filePublish := filepath.Join(relativePathPub, "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish mpublish.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = m1

	require.NoError(t, channel.Publish(messagePublish, nil))
}

func TestLAOChannel_Publish_LaoState(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
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

	m1 := mmessage.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufb64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	// Store the update lao in the inbox
	laoChannel.inbox.StoreMessage(m1)

	// Create a lao_state message
	relativePathState := filepath.Join(protocolRelativePath,
		"examples", "messageData", "lao_state")

	fileState := filepath.Join(relativePathState, "lao_state.json")
	bufState, err := os.ReadFile(fileState)
	require.NoError(t, err)

	var mState mlao.LaoState
	err = json.Unmarshal(bufState, &mState)
	require.NoError(t, err)

	mState.ModificationID = mmessage.Hash(bufb64, publicKey64)
	mState.ModificationSignatures = []mlao.ModificationSignature{}

	mStateBuf, err := json.Marshal(mState)
	require.NoError(t, err)

	bufState64 := base64.URLEncoding.EncodeToString(mStateBuf)

	m2 := mmessage.Message{
		Data:              bufState64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufState64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePathStatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileStatePublish := filepath.Join(relativePathStatePub, "publish.json")
	bufStatePub, err := os.ReadFile(fileStatePublish)
	require.NoError(t, err)

	var messageStatePublish mpublish.Publish

	err = json.Unmarshal(bufStatePub, &messageStatePublish)
	require.NoError(t, err)

	messageStatePublish.Params.Message = m2

	require.NoError(t, channel.Publish(messageStatePublish, nil))
}

func TestBaseChannel_ConsensusIsCreated(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}

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
	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}

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

	m1 := mmessage.Message{
		Data:              bufCreate64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufCreate64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePathCreatePub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var messageCreatePub mpublish.Publish

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

	m2 := mmessage.Message{
		Data:              bufOpen64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufOpen64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
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

	m3 := mmessage.Message{
		Data:              bufClose64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufClose64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	messageClosePub := messageCreatePub
	messageClosePub.Params.Message = m3

	require.NoError(t, channel.Publish(messageClosePub, nil))
}

func TestLAOChannel_Rollcall_Creation_Not_Organizer(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel(sampleLao, fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	// Publish a rollcall create message with a different key than the
	// organizer, an error is expected
	err = channel.Publish(sampleRollCallCreatePublish, nil)
	require.Error(t, err)
}

func TestLAOChannel_Rollcall_Open_Not_Organizer(t *testing.T) {
	keypairOrg := generateKeyPair(t)
	keypairOther := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypairOrg.public, nolog, nil)
	require.NoError(t, err)

	laoId := mmessage.Hash(base64.URLEncoding.EncodeToString(keypairOrg.
		publicBuf), strconv.FormatInt(time.Now().Unix(), 10), "Lao 1")
	laoChannel := "/root/" + laoId

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel(laoChannel, fakeHub, m, nolog, keypairOrg.public, nil)
	require.NoError(t, err)

	rollcallCreate, rollcallId := createRollCallCreate(t, keypairOrg, laoId)

	err = channel.Publish(rollcallCreate, nil)
	require.NoError(t, err)

	rollcallOpen := createRollCallOpen(t, keypairOther, laoId, rollcallId)

	// Publish a rollcall open message with a different key than the
	// organizer, an error is expected
	err = channel.Publish(rollcallOpen, nil)
	require.Error(t, err)
}

func TestLAOChannel_Rollcall_Close_Not_Organizer(t *testing.T) {
	keypairOrg := generateKeyPair(t)
	keypairOther := generateKeyPair(t)

	fakeHub, err := NewFakeHub("", keypairOrg.public, nolog, nil)
	require.NoError(t, err)

	laoId := mmessage.Hash(base64.URLEncoding.EncodeToString(keypairOrg.
		publicBuf), strconv.FormatInt(time.Now().Unix(), 10), "Lao 1")
	laoChannel := "/root/" + laoId

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel(laoChannel, fakeHub, m, nolog, keypairOrg.public, nil)
	require.NoError(t, err)

	rollcallCreate, rollcallId := createRollCallCreate(t, keypairOrg, laoId)

	err = channel.Publish(rollcallCreate, nil)
	require.NoError(t, err)

	rollcallOpen := createRollCallOpen(t, keypairOrg, laoId, rollcallId)
	err = channel.Publish(rollcallOpen, nil)
	require.NoError(t, err)

	rollcallClose := createRollCallClose(t, keypairOther, laoId, rollcallId)

	// Publish a rollcall close message with a different key than the
	// organizer, an error is expected
	err = channel.Publish(rollcallClose, nil)
	require.Error(t, err)
}

func TestLAOChannel_Election_Creation(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewFakeHub("", keypair.public, nolog, nil)
	require.NoError(t, err)

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel("/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	// Create an update lao message
	relativePath := filepath.Join(protocolRelativePath,
		"examples", "messageData")

	file := filepath.Join(relativePath, "election_setup", "election_setup.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	m1 := mmessage.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         mmessage.Hash(bufb64, publicKey64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePathPub := filepath.Join(protocolRelativePath,
		"examples", "query", "publish")

	filePublish := filepath.Join(relativePathPub, "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish mpublish.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = m1

	require.NoError(t, channel.Publish(messagePublish, nil))
}

func TestLAOChannel_Sends_Greeting(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewFakeHub("ws://localhost:9000/client", keypair.public, nolog, nil)
	require.NoError(t, err)

	peerAddresses := []string{}
	for _, serverInfo := range fakeHub.GetPeersInfo() {
		peerAddresses = append(peerAddresses, serverInfo.ClientAddress)
	}

	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel("/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", fakeHub, m, nolog, keypair.public, nil)
	require.NoError(t, err)

	catchupAnswer := channel.Catchup(mcatchup.Catchup{ID: 0})
	// should contain the creation message and the LAO greet
	require.Len(t, catchupAnswer, 2)

	greetMsg := catchupAnswer[1]

	var laoGreet mlao.LaoGreet

	err = greetMsg.UnmarshalData(&laoGreet)
	require.NoError(t, err)

	require.Equal(t, mmessage.LAOObject, laoGreet.Object)
	require.Equal(t, mmessage.LAOActionGreet, laoGreet.Action)
	require.Equal(t, "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=", laoGreet.LaoID)
	require.Equal(t, publicKey64, laoGreet.Frontend)
	require.Equal(t, "ws://localhost:9000/client", laoGreet.Address)
	for _, peer := range laoGreet.Peers {
		require.Contains(t, peerAddresses, peer.Address)
	}
}

func Test_LAOChannel_Witness_Message(t *testing.T) {
	organizerPk := getPublicKeyPoint(t, organizerPublicKey)

	fakeHub, err := NewFakeHub("", organizerPk, nolog, nil)
	require.NoError(t, err)

	// Create new Lao channel
	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel(sampleLao, fakeHub, m, nolog, organizerPk, nil)
	require.NoError(t, err)

	// Publish roll_call_create message
	require.NoError(t, channel.Publish(sampleRollCallCreatePublish, nil))

	// Publish witness message and catchup on channel to get the message back
	require.NoError(t, channel.Publish(sampleWitnessMessagePublish, nil))
	catchupAnswer := channel.Catchup(mcatchup.Catchup{ID: 0})

	// Check that the witness signature was added to the message
	require.Equal(t, 1, len(catchupAnswer[2].WitnessSignatures))
}

func Test_LAOChannel_Witness_Message_Not_Received_Yet(t *testing.T) {
	organizerPk := getPublicKeyPoint(t, organizerPublicKey)

	fakeHub, err := NewFakeHub("", organizerPk, nolog, nil)
	require.NoError(t, err)

	// Create new Lao channel
	m := mmessage.Message{MessageID: "0"}
	channel, err := NewChannel(sampleLao, fakeHub, m, nolog, organizerPk, nil)
	require.NoError(t, err)

	// Publish witness message and catchup on channel to get the message back
	require.NoError(t, channel.Publish(sampleWitnessMessagePublish, nil))

	// Publish roll_call_create message
	require.NoError(t, channel.Publish(sampleRollCallCreatePublish, nil))

	catchupAnswer := channel.Catchup(mcatchup.Catchup{ID: 0})

	// Check that the witness signature was added to the message
	require.Equal(t, 1, len(catchupAnswer[3].WitnessSignatures))
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
	clientAddress string

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
func NewFakeHub(clientAddress string, publicOrg kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*fakeHub, error) {

	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := fakeHub{
		clientAddress:   clientAddress,
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

// GetClientServerAddress implements channel.HubFunctionalities
func (h *fakeHub) GetClientServerAddress() string {
	return h.clientAddress
}

// Sign implements channel.HubFunctionalities
func (h *fakeHub) Sign(data []byte) ([]byte, error) {
	return nil, nil
}

// NotifyWitnessMessage implements channel.HubFunctionalities
func (h *fakeHub) NotifyWitnessMessage(messageId string, publicKey string, signature string) {}

// GetPeersInfo implements channel.HubFunctionalities
func (h *fakeHub) GetPeersInfo() []mgreetserver.GreetServerParams {
	peer1 := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ClientAddress: "wss://localhost:9002/client",
		ServerAddress: "",
	}

	peer2 := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ClientAddress: "wss://localhost:9004/client",
		ServerAddress: "",
	}
	return []mgreetserver.GreetServerParams{peer1, peer2}
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

func (h *fakeHub) GetServerNumber() int {
	return 0
}

func (h *fakeHub) SendAndHandleMessage(msg mbroadcast.Broadcast) error {
	return nil
}

// fakeSocket is a fake implementation of a Socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID int
	res      []mmessage.Message
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
func (f *fakeSocket) SendResult(id int, res []mmessage.Message, missingMsgs map[string][]mmessage.Message) {
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

// -----------------------------------------------------------------------------
// Useful data extracted from a simulation

var sampleLao = "/root/QNNTcGQk-rnehNjgizdzi9IT1nIlmXsOXy1BCWsNaVE="
var organizerPublicKey = "A2nPAZfsvBRPb5uOb1_hUVuAKt5YKPRZdiFq1g0TLr0="

// getPublicKeyPoint convert a base64 encoded public key to a Kyber.Point
func getPublicKeyPoint(t *testing.T, publicKeyBase64 string) kyber.Point {
	keyBuf, err := base64.URLEncoding.DecodeString(publicKeyBase64)
	require.NoError(t, err)

	senderPk := crypto.Suite.Point()

	err = senderPk.UnmarshalBinary(keyBuf)
	require.NoError(t, err)

	return senderPk
}

var sampleRollCallCreate = mmessage.Message{
	Data: "eyJjcmVhdGlvbiI6MTY4NDI1OTU4MSwiZGVzY3JpcHRpb24iOiIiLCJpZCI6IktxLV9CbUJUZTFEWnFjSXEzU2pOcklzdHAzTFdCM0N6VFhoOVpBaHctUUU9IiwibG9jYXRpb24iOiJ0ZSI" +
		"sIm5hbWUiOiJ0ZSIsInByb3Bvc2VkX2VuZCI6MTY4NDI2MzEyMCwicHJvcG9zZWRfc3RhcnQiOjE2ODQyNTk1ODEsIm9iamVjdCI6InJvbGxfY2FsbCIsImFjdGlvbiI6ImNyZWF0ZSJ9",
	Sender:            organizerPublicKey,
	Signature:         "j-7dykTLzS0qSPBiuQyxULXvWoPf-To89-vjnOtKyj9po2EjtNeUStgrK79OJOi8LYt6MmPCl6GVC8gzvhW-AA==",
	MessageID:         "JEZPhpKgQZ_ZFEncCapUozRdeepMXV8N0Zeyz7EFfNU=",
	WitnessSignatures: nil,
}

var sampleRollCallCreatePublish = mpublish.Publish{
	Base: mquery.Base{
		JSONRPCBase: jsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		Method: "publish",
	},
	Params: struct {
		Channel string           `json:"channel"`
		Message mmessage.Message `json:"message"`
	}{
		Channel: sampleLao,
		Message: sampleRollCallCreate,
	},
}

var sampleWitnessMessage = mmessage.Message{
	Data: "eyJtZXNzYWdlX2lkIjoiSkVaUGhwS2dRWl9aRkVuY0NhcFVvelJkZWVwTVhWOE4wWmV5ejdFRmZOVT0iLCJzaWduYXR1cmUiOiJfR1lXZkJqWlEzZy1EQTVrTjNRdngxYkpRRlBOS2Zy" +
		"T0lpTXJ1RnF5T2VjaldzZ0dkWTk3ek04M214VlFxUnVHUHhCR1Mwd1N2bEtJTHplaFpSTWNBQT09Iiwib2JqZWN0IjoibWVzc2FnZSIsImFjdGlvbiI6IndpdG5lc3MifQ==",
	Sender:    organizerPublicKey,
	Signature: "3tHH0Km-LBfbBvqVLDjW_mHTTckAVfZHl-6NG55eWpVdk8tOnUVxbgdeNK3eC44MpKZFS7d4GdR86HJmFAjNAA==",
	MessageID: "FBVnOu7SeIXWUstgFyPkdHmnv36dtxLE7yb8n4v1D6k=",
}

var sampleWitnessMessagePublish = mpublish.Publish{
	Base: mquery.Base{
		JSONRPCBase: jsonrpc.JSONRPCBase{
			JSONRPC: "2.0",
		},
		Method: "publish",
	},
	Params: struct {
		Channel string           `json:"channel"`
		Message mmessage.Message `json:"message"`
	}{
		Channel: sampleLao,
		Message: sampleWitnessMessage,
	},
}

// createPublish is a helper function that create a Publish message
// containing a message data with valid signature and ids
func createPublish(t *testing.T, sender keypair, laoId string,
	data []byte) mpublish.Publish {

	data64 := base64.URLEncoding.EncodeToString(data)
	senderPk := base64.URLEncoding.EncodeToString(sender.publicBuf)
	signature, err := schnorr.Sign(suite, sender.private, data)
	require.NoError(t, err)

	msg := mmessage.Message{
		Data:              data64,
		Sender:            senderPk,
		Signature:         base64.URLEncoding.EncodeToString(signature),
		MessageID:         mmessage.Hash(data64, senderPk),
		WitnessSignatures: nil,
	}

	publishMsg := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
		}{
			Channel: "/root/" + laoId,
			Message: msg,
		},
	}

	return publishMsg
}

func createRollCallCreate(t *testing.T, sender keypair,
	laoId string) (mpublish.Publish, string) {

	now := time.Now().Unix()
	rollcallName := "Roll Call"
	rollcallId := mmessage.Hash("R", laoId, strconv.FormatInt(now, 10), rollcallName)
	rollcallCreate, err := json.Marshal(mlao.RollCallCreate{
		Object:        "roll_call",
		Action:        "create",
		ID:            rollcallId,
		Name:          rollcallName,
		Creation:      now,
		ProposedStart: now,
		ProposedEnd:   now + 1000,
		Location:      "EPFL",
		Description:   "",
	})
	require.NoError(t, err)

	rollcallCreatePublish := createPublish(t, sender, laoId, rollcallCreate)

	return rollcallCreatePublish, rollcallId
}

func createRollCallOpen(t *testing.T, sender keypair,
	laoId string, rollcallId string) mpublish.Publish {

	openAt := time.Now().Unix()
	updateId := mmessage.Hash("R", laoId, rollcallId, strconv.FormatInt(openAt, 10))

	rollcallOpen, err := json.Marshal(mlao.RollCallOpen{
		Object:   "roll_call",
		Action:   "open",
		UpdateID: updateId,
		Opens:    rollcallId,
		OpenedAt: openAt,
	})
	require.NoError(t, err)

	rollcallOpenPublish := createPublish(t, sender, laoId, rollcallOpen)

	return rollcallOpenPublish
}

func createRollCallClose(t *testing.T, sender keypair,
	laoId string, openId string) mpublish.Publish {

	closeAt := time.Now().Unix()
	updateId := mmessage.Hash("R", laoId, openId, strconv.FormatInt(closeAt, 10))
	attendees := []string{base64.URLEncoding.EncodeToString(sender.publicBuf)}

	rollcallClose, err := json.Marshal(mlao.RollCallClose{
		Object:    "roll_call",
		Action:    "close",
		UpdateID:  updateId,
		Closes:    openId,
		ClosedAt:  closeAt,
		Attendees: attendees,
	})
	require.NoError(t, err)

	rollcallClosePublish := createPublish(t, sender, laoId, rollcallClose)

	return rollcallClosePublish
}
