package chirp

import (
	"encoding/base64"
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
	"popstellar/channel/generalChirping"
	"popstellar/crypto"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
	"time"
)

const laoID = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
const sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
const root = "/root/"
const social = "/social/"
const posts = "posts"

func Test_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	generalName := root + laoID + social + posts
	chirpChannelName := root + laoID + social + sender
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)
	// Create the channel
	cha := NewChannel(chirpChannelName, sender, fakeHub, &generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, &generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, &cha)

	_, found := fakeHub.channelByID[root+laoID+social+sender]
	require.True(t, found)
	_, found = fakeHub.channelByID[root+laoID+social+posts]
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

func Test_SendChirp(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)
	generalName := root + laoID + social + posts
	chirpChannelName := root + laoID + social + sender
	generalCha := generalChirping.NewChannel(generalName, fakeHub, nolog)
	// Create the channel
	cha := NewChannel(chirpChannelName, sender, fakeHub, &generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, &generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, &cha)
	_, found := fakeHub.channelByID[root+laoID+social+sender]
	require.True(t, found)
	_, found = fakeHub.channelByID[root+laoID+social+posts]
	require.True(t, found)

	time.Sleep(time.Millisecond)

	// Create the message
	relativePath := filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData")

	file := filepath.Join(relativePath, "chirp_add_publish.json")
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

	relativePathCreatePub := filepath.Join("..", "..", "..", "protocol",
		"examples", "query", "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var message method.Publish

	err = json.Unmarshal(bufCreatePub, &message)
	require.NoError(t, err)

	message.Params.Message = m
	message.Params.Channel = root + laoID + social + sender

	require.NoError(t, cha.Publish(message, socket.ClientSocket{}))

	msg := generalCha.Catchup(method.Catchup{ID: 0})

	checkData := messagedata.ChirpBroadcast{
		Object:    "chirp",
		Action:    "add_broadcast",
		ChirpId:   messagedata.Hash(buf64, "h"),
		Channel:   root + laoID + social + posts,
		Timestamp: 123,
	}

	checkDataBuf, err := json.Marshal(checkData)
	require.Nil(t, err)
	checkData64 := base64.URLEncoding.EncodeToString(checkDataBuf)

	// check if the data on the general is the same as the one we sent
	require.Equal(t, checkData64, msg[0].Data)
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
