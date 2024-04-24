package federation

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
	"io"
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
)

const (
	localServerAddress  = "ws://localhost:19008/client"
	remoteServerAddress = "ws://localhost:19009/client"
	localLaoId          = "JYYWSfI2Au1lS7gGAZCUueY9PRMtu3ltKOFLjsdQs7s="
	remoteLaoId         = "ztPxKxfhToSloYcruyjfurcFD3sfDJ2B3o9l6v7Erho="
	localLaoChannel     = "/root/" + localLaoId
	localFedChannel     = localLaoChannel + "/federation"
)

// TestChannel_FederationRequestChallenge tests that a FederationChallenge is
// received after a valid FederationChallengeRequest is published by the
// organizer
func Test_FederationRequestChallenge(t *testing.T) {
	organizerKeypair := generateKeyPair(t)
	fakeHub, err := NewFakeHub("", organizerKeypair.public, nolog, nil)
	require.NoError(t, err)

	fedChannel := NewChannel(localFedChannel, fakeHub, nolog,
		organizerKeypair.publicKey)

	requestData := messagedata.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
		Timestamp: time.Now().Unix(),
	}

	requestMsg := generateMessage(t, organizerKeypair, requestData)
	publishMsg := generatePublish(t, localFedChannel, requestMsg)
	socket := &fakeSocket{id: "sockSocket"}

	err = fedChannel.Publish(publishMsg, socket)
	require.NoError(t, err)

	require.NoError(t, socket.err)
	require.NotNil(t, socket.msg)

	var challengePublish method.Publish
	err = json.Unmarshal(socket.msg, &challengePublish)
	require.NoError(t, err)

	require.Equal(t, localFedChannel, challengePublish.Params.Channel)
	challengeMsg := challengePublish.Params.Message

	dataBytes, err := base64.URLEncoding.DecodeString(challengeMsg.Data)
	require.NoError(t, err)
	signatureBytes, err := base64.URLEncoding.DecodeString(challengeMsg.Signature)
	require.NoError(t, err)

	err = schnorr.Verify(crypto.Suite, fakeHub.pubKeyServ, dataBytes, signatureBytes)
	require.NoError(t, err)

	var challenge messagedata.FederationChallenge
	err = challengeMsg.UnmarshalData(&challenge)
	require.NoError(t, err)

	require.Equal(t, messagedata.FederationObject, challenge.Object)
	require.Equal(t, messagedata.FederationActionChallenge, challenge.Action)
	require.Greater(t, challenge.Timestamp, time.Now().Unix())
	bytes, err := hex.DecodeString(challenge.Value)
	require.NoError(t, err)
	require.Len(t, bytes, 32)
}

func Test_FederationRequestChallenge_not_organizer(t *testing.T) {
	organizerKeypair := generateKeyPair(t)
	notOrganizerKeypair := generateKeyPair(t)
	fakeHub, err := NewFakeHub("", organizerKeypair.public, nolog, nil)
	require.NoError(t, err)

	fedChannel := NewChannel(localFedChannel, fakeHub, nolog,
		organizerKeypair.publicKey)

	requestData := messagedata.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
		Timestamp: time.Now().Unix(),
	}

	requestMsg := generateMessage(t, notOrganizerKeypair, requestData)
	publishMsg := generatePublish(t, localFedChannel, requestMsg)
	socket := &fakeSocket{id: "sockSocket"}

	err = fedChannel.Publish(publishMsg, socket)
	require.Error(t, err)

	require.Nil(t, socket.msg)
}

func Test_FederationExpect(t *testing.T) {
	organizerKeypair := generateKeyPair(t)
	fakeHub, err := NewFakeHub("", organizerKeypair.public, nolog, nil)
	require.NoError(t, err)

	fedChannel := NewChannel(localFedChannel, fakeHub, nolog,
		organizerKeypair.publicKey)

	requestData := messagedata.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
		Timestamp: time.Now().Unix(),
	}

	requestMsg := generateMessage(t, organizerKeypair, requestData)
	publishMsg := generatePublish(t, localFedChannel, requestMsg)
	socket := &fakeSocket{id: "sockSocket"}

	err = fedChannel.Publish(publishMsg, socket)
	require.NoError(t, err)

	require.NoError(t, socket.err)
	require.NotNil(t, socket.msg)

	var challengePublish method.Publish
	err = json.Unmarshal(socket.msg, &challengePublish)
	require.NoError(t, err)

	require.Equal(t, localFedChannel, challengePublish.Params.Channel)
	challengeMsg := challengePublish.Params.Message

	var challenge messagedata.FederationChallenge
	err = challengeMsg.UnmarshalData(&challenge)
	require.NoError(t, err)

	remoteOrganizerKeypair := generateKeyPair(t)

	federationExpect := messagedata.FederationExpect{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
		LaoId:         remoteLaoId,
		ServerAddress: remoteServerAddress,
		PublicKey:     remoteOrganizerKeypair.publicKey,
		Challenge: messagedata.Challenge{
			Value:      challenge.Value,
			ValidUntil: challenge.Timestamp,
		},
	}

	federationMsg := generateMessage(t, organizerKeypair, federationExpect)
	federationPublish := generatePublish(t, localFedChannel, federationMsg)

	err = fedChannel.Publish(federationPublish, socket)
	require.NoError(t, err)

	require.NoError(t, socket.err)
	require.NotNil(t, socket.msg)
}

func Test_FederationExpect_with_invalid_challenge(t *testing.T) {
	organizerKeypair := generateKeyPair(t)
	fakeHub, err := NewFakeHub("", organizerKeypair.public, nolog, nil)
	require.NoError(t, err)

	fedChannel := NewChannel(localFedChannel, fakeHub, nolog,
		organizerKeypair.publicKey)

	requestData := messagedata.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
		Timestamp: time.Now().Unix(),
	}

	requestMsg := generateMessage(t, organizerKeypair, requestData)
	publishMsg := generatePublish(t, localFedChannel, requestMsg)
	socket := &fakeSocket{id: "sockSocket"}

	err = fedChannel.Publish(publishMsg, socket)
	require.NoError(t, err)

	require.NoError(t, socket.err)
	require.NotNil(t, socket.msg)

	remoteOrganizerKeypair := generateKeyPair(t)

	valueBytes := make([]byte, 32)
	_, _ = rand.Read(valueBytes)

	federationExpect := messagedata.FederationExpect{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
		LaoId:         remoteLaoId,
		ServerAddress: remoteServerAddress,
		PublicKey:     remoteOrganizerKeypair.publicKey,
		Challenge: messagedata.Challenge{
			Value:      hex.EncodeToString(valueBytes),
			ValidUntil: time.Now().Unix(),
		},
	}

	expectMsg := generateMessage(t, organizerKeypair, federationExpect)
	expectPublish := generatePublish(t, localFedChannel, expectMsg)

	err = fedChannel.Publish(expectPublish, socket)
	require.Error(t, err)
}

func Test_FederationChallenge_not_organizer(t *testing.T) {
	organizerKeypair := generateKeyPair(t)
	notOrganizerKeypair := generateKeyPair(t)
	fakeHub, err := NewFakeHub("", organizerKeypair.public, nolog, nil)
	require.NoError(t, err)

	fedChannel := NewChannel(localFedChannel, fakeHub, nolog,
		organizerKeypair.publicKey)

	requestData := messagedata.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
		Timestamp: time.Now().Unix(),
	}

	requestMsg := generateMessage(t, organizerKeypair, requestData)
	publishMsg := generatePublish(t, localFedChannel, requestMsg)
	socket := &fakeSocket{id: "sockSocket"}

	err = fedChannel.Publish(publishMsg, socket)
	require.NoError(t, err)

	require.NoError(t, socket.err)
	require.NotNil(t, socket.msg)

	var challengePublish method.Publish
	err = json.Unmarshal(socket.msg, &challengePublish)
	require.NoError(t, err)

	require.Equal(t, localFedChannel, challengePublish.Params.Channel)
	challengeMsg := challengePublish.Params.Message

	var challenge messagedata.FederationChallenge
	err = challengeMsg.UnmarshalData(&challenge)
	require.NoError(t, err)

	remoteOrganizerKeypair := generateKeyPair(t)

	federationExpect := messagedata.FederationExpect{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
		LaoId:         remoteLaoId,
		ServerAddress: remoteServerAddress,
		PublicKey:     remoteOrganizerKeypair.publicKey,
		Challenge: messagedata.Challenge{
			Value:      challenge.Value,
			ValidUntil: challenge.Timestamp,
		},
	}

	federationMsg := generateMessage(t, notOrganizerKeypair, federationExpect)
	federationPublish := generatePublish(t, localFedChannel, federationMsg)

	err = fedChannel.Publish(federationPublish, socket)
	require.Error(t, err)
}

func Test_FederationInit(t *testing.T) {

}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicKey string
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)
var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	pkBuf, err := point.MarshalBinary()
	require.NoError(t, err)

	pkBase64 := base64.URLEncoding.EncodeToString(pkBuf)

	return keypair{point, pkBase64, secret}
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

	schemaValidator, err := validation.NewSchemaValidator(log)
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
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}

	return signatureBuf, nil
}

// NotifyWitnessMessage implements channel.HubFunctionalities
func (h *fakeHub) NotifyWitnessMessage(messageId string, publicKey string, signature string) {}

// GetPeersInfo implements channel.HubFunctionalities
func (h *fakeHub) GetPeersInfo() []method.ServerInfo {
	return nil
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

func (h *fakeHub) ConnectToServerAsClient(serverAddress string) (*socket.ClientSocket, error) {
	return nil, nil
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

func generatePublish(t *testing.T, channel string, msg message.Message) method.
	Publish {
	return method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: channel,
			Message: msg,
		},
	}
}

func generateMessage(t *testing.T, keys keypair,
	data messagedata.MessageData) message.Message {

	dataBytes, err := json.Marshal(data)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBytes)

	signatureBytes, err := schnorr.Sign(crypto.Suite, keys.private, dataBytes)
	signatureBase64 := base64.URLEncoding.EncodeToString(signatureBytes)

	return message.Message{
		Data:              dataBase64,
		Sender:            keys.publicKey,
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
}
