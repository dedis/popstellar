package hub

import (
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/hub/state"
	"popstellar/inbox"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"popstellar/storage"
	"popstellar/validation"
	"strings"
	"sync"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	// rootChannel denotes the id of the root channel
	rootChannel = "/root"

	// rootPrefix denotes the prefix for the root channel
	// used to keep an image of the laos
	rootPrefix = rootChannel + "/"

	// Strings used to return error messages
	rootChannelErr = "failed to handle root channel message: %v"
	getChannelErr  = "failed to get channel: %v"

	// numWorkers denote the number of worker go-routines
	// allowed to process requests concurrently.
	numWorkers = 10

	// heartbeatDelay represents the number of seconds
	// between heartbeat messages
	heartbeatDelay = 30 * time.Second

	publishError        = "failed to publish: %v"
	wrongMessageIdError = "message_id is wrong: expected %q found %q"
	maxRetry            = 10
)

var suite = crypto.Suite

// Huber defines the methods a PoP server must implement to receive messages
// and handle clients.
type Huber interface {
	// NotifyNewServer add a socket for the hub to send message to other servers
	NotifyNewServer(socket.Socket)

	// Start invokes the processing loop for the hub.
	Start()

	// Stop closes the processing loop for the hub.
	Stop()

	// Receiver returns a channel that may be used to process incoming messages
	Receiver() chan<- socket.IncomingMessage

	// OnSocketClose returns a channel which accepts socket ids on connection
	// close events. This allows the hub to cleanup clients which close without
	// sending an unsubscribe message
	OnSocketClose() chan<- string

	// SendGreetServer sends a greet server message in the socket
	SendGreetServer(socket.Socket) error
}

type subscribers map[string]map[socket.Socket]struct{}

type handlerParameters struct {
	socket          socket.Socket
	schemaValidator *validation.SchemaValidator
	db              storage.Storage
	subs            subscribers
	peers           *state.Peers
}

// Hub implements the Hub interface.
type Hub struct {
	clientServerAddress string
	serverServerAddress string

	subs subscribers

	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID state.Channels

	closedSockets chan string

	pubKeyOwner kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	log zerolog.Logger

	laoFac channel.LaoFactory

	serverSockets channel.Sockets

	// hubInbox is used to remember the messages that the hub received
	hubInbox inbox.HubInbox

	// queries are used to help servers catchup to each other
	queries state.Queries

	// peers stores information about the peers
	peers state.Peers

	// blacklist stores the IDs of the messages that failed to be processed by the hub
	// the server will not ask for them again in the heartbeat
	// and will not process them if they are received again
	// @TODO remove the messages from the blacklist after a certain amount of time by trying to process them again
	blacklist state.ThreadSafeSlice[string]
}

// New returns a new Hub.
func New(pubKeyOwner kyber.Point, clientServerAddress string, serverServerAddress string, log zerolog.Logger,
	laoFac channel.LaoFactory,
) (*Hub, error) {
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := Hub{
		clientServerAddress: clientServerAddress,
		serverServerAddress: serverServerAddress,
		subs:                make(subscribers),
		messageChan:         make(chan socket.IncomingMessage),
		channelByID:         state.NewChannelsMap(),
		closedSockets:       make(chan string),
		pubKeyOwner:         pubKeyOwner,
		pubKeyServ:          pubServ,
		secKeyServ:          secServ,
		schemaValidator:     schemaValidator,
		stop:                make(chan struct{}),
		log:                 log,
		laoFac:              laoFac,
		serverSockets:       channel.NewSockets(),
		hubInbox:            *inbox.NewHubInbox(rootChannel),
		queries:             state.NewQueries(log),
		peers:               state.NewPeers(),
		blacklist:           state.NewThreadSafeSlice[string](),
	}

	return &hub, nil
}

// Start implements hub.Hub
func (h *Hub) Start() {
	go func() {
		ticker := time.NewTicker(heartbeatDelay)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				h.sendHeartbeatToServers()
			case <-h.stop:
				h.log.Info().Msg("stopping the hub")
				return
			}
		}
	}()

	go func() {
		h.log.Info().Msg("Start check messages")
		for {
			select {
			case incomingMessage := <-h.messageChan:
				err := h.handleIncomingMessage(&incomingMessage)
				if err != nil {
					h.log.Err(err).Msg("problem handling incoming message")
				}
			case id := <-h.closedSockets:
				h.channelByID.ForEach(func(c channel.Channel) {
					// dummy Unsubscribe message because it's only used for logging...
					c.Unsubscribe(id, method.Unsubscribe{})
				})
			case <-h.stop:
				h.log.Info().Msg("stopping the hub")
				return
			}
		}
	}()
}

// Stop implements hub.Hub
func (h *Hub) Stop() {
	close(h.stop)
}

// Receiver implements hub.Hub
func (h *Hub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

// NotifyNewServer adds a socket to the sockets known by the hub
func (h *Hub) NotifyNewServer(socket socket.Socket) {
	h.serverSockets.Upsert(socket)
}

// GetServerNumber returns the number of servers known by this one
func (h *Hub) GetServerNumber() int {
	// serverSockets + 1 as the server also know itself
	return h.serverSockets.Len() + 1
}

// OnSocketClose implements hub.Hub
func (h *Hub) OnSocketClose() chan<- string {
	return h.closedSockets
}

func (h *Hub) getChan(channelPath string) (channel.Channel, error) {
	if !strings.HasPrefix(channelPath, rootPrefix) {
		return nil, xerrors.Errorf("channel not prefixed with '%s': %q", rootPrefix, channelPath)
	}

	channel, ok := h.channelByID.Get(channelPath)
	if !ok {
		return nil, xerrors.Errorf("channel %s does not exist", channelPath)
	}

	return channel, nil
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *Hub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) error {
	h.log.Info().Str("msg", string(incomingMessage.Message)).Msg("handle incoming message")

	switch incomingMessage.Socket.Type() {
	case socket.ClientSocketType:
		return h.handleMessageFromClient(incomingMessage)
	case socket.ServerSocketType:
		return h.handleMessageFromServer(incomingMessage)
	default:
		return xerrors.Errorf("invalid socket type")
	}
}

// GetPubKeyOwner implements channel.HubFunctionalities
func (h *Hub) GetPubKeyOwner() kyber.Point {
	return h.pubKeyOwner
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *Hub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
}

// GetClientServerAddress implements channel.HubFunctionalities
func (h *Hub) GetClientServerAddress() string {
	return h.clientServerAddress
}

// Sign implements channel.HubFunctionalities
func (h *Hub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}

	return signatureBuf, nil
}

// GetSchemaValidator implements channel.HubFunctionalities
func (h *Hub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

// NotifyNewChannel implements channel.HubFunctionalities
func (h *Hub) NotifyNewChannel(channelID string, channel channel.Channel, sock socket.Socket) {
	h.channelByID.Set(channelID, channel)
}

// NotifyWitnessMessage implements channel.HubFunctionalities
func (h *Hub) NotifyWitnessMessage(messageId string, publicKey string, signature string) {
	h.hubInbox.AddWitnessSignature(messageId, publicKey, signature)
}

func (h *Hub) GetPeersInfo() []method.ServerInfo {
	return h.peers.GetAllPeersInfo()
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

func Sign(data []byte, params handlerParameters) ([]byte, error) {

	serverSecretBuf, err := params.db.GetServerSecretKey()
	if err != nil {
		return nil, xerrors.Errorf("failed to get the server secret key")
	}

	serverSecretKey := crypto.Suite.Scalar()
	err = serverSecretKey.UnmarshalBinary(serverSecretBuf)
	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}
