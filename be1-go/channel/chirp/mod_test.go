package chirp

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
	"io"
	"popstellar/channel"
	generalChriping "popstellar/channel/generalChirping"
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

func Test_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)
	laoID := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=";
	sender := "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=";

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)
	generalName := "/root/" + laoID + "/social/posts"
	chirpChannelName := "/root/" + laoID + "/social/" + sender
	generalCha := generalChriping.NewChannel(generalName, fakeHub, nolog)
	// Create the channel
	cha := NewChannel(chirpChannelName, sender, fakeHub, &generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, &generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, &cha)
	_, found := fakeHub.channelByID["/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="]
	require.True(t, found)
	_, found = fakeHub.channelByID["/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/posts"]
	require.True(t, found)

	time.Sleep(time.Millisecond)

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
	laoID := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=";
	sender := "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=";

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)
	generalName := "/root/" + laoID + "/social/posts"
	chirpChannelName := "/root/" + laoID + "/social/" + sender
	generalCha := generalChriping.NewChannel(generalName, fakeHub, nolog)
	// Create the channel
	cha := NewChannel(chirpChannelName, sender, fakeHub, &generalCha, nolog)

	fakeHub.RegisterNewChannel(generalName, &generalCha)
	fakeHub.RegisterNewChannel(chirpChannelName, &cha)
	_, found := fakeHub.channelByID["/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="]
	require.True(t, found)
	_, found = fakeHub.channelByID["/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/posts"]
	require.True(t, found)

	time.Sleep(time.Millisecond)

	// Create the message
	newDataCreate := messagedata.ChirpAdd{
		Object: "chirp",
		Action: "add",
		Text: "testing is rewarding",
		Timestamp: 1633098853,
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
			"/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=",
			message.Message{
				Data: newData64Create,
				Sender: "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=",
				Signature: "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw==",
				MessageID: messagedata.Hash(newData64Create, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw=="),
				WitnessSignatures: []message.WitnessSignature{},
			},
		},
	}

	err = cha.Publish(rpcMessageCreate)
	require.NoError(t, err)

	msg := generalCha.Catchup(method.Catchup{ID: 0})

	checkData := messagedata.ChirpBroadcast{
		Object: "chirp",
		Action: "addBroadcast",
		ChirpId: messagedata.Hash(newData64Create, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw=="),
		Channel: "/root/fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/social/posts",
		Timestamp: 1633098853,
	}

	checkDataBuf, err := json.Marshal(checkData)
	require.Nil(t, err)
	checkData64 := base64.URLEncoding.EncodeToString(checkDataBuf)

	// check if the data on the general is the same as the one we sent
	require.Equal(t, checkData64, msg[0].Data)
}

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