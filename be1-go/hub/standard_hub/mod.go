package standard_hub

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"golang.org/x/exp/slices"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/inbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strings"
	"sync"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
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
)

var suite = crypto.Suite

// Hub implements the Hub interface.
type Hub struct {
	clientServerAddress string
	serverServerAddress string

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

	serverSockets channel.Sockets

	// hubInbox is used to remember the messages that the hub received
	hubInbox inbox.Inbox

	// rootInbox and queries are used to help servers catchup to each other
	rootInbox inbox.Inbox
	queries   queries

	// messageIdsByChannel stores all the message ids and the corresponding channel ids
	// to help servers determine in which channel the message ids go
	messageIdsByChannel map[string][]string

	// peersInfo stores the info of the peers: public key, client and server endpoints associated with the socket ID
	peersInfo map[string]method.ServerInfo

	// peersGreeted stores the peers that were greeted by the socket ID
	peersGreeted []string
}

// newQueries creates a new queries struct
func newQueries() queries {
	return queries{
		state:                  make(map[int]*bool),
		getMessagesByIdQueries: make(map[int]method.GetMessagesById),
	}
}

// queries let the hub remember all queries that it sent to other servers
type queries struct {
	sync.Mutex
	// state stores the ID of the server's queries and their state. False for a
	// query not yet answered, else true.
	state map[int]*bool
	// getMessagesByIdQueries stores the server's getMessagesByIds queries by their ID.
	getMessagesByIdQueries map[int]method.GetMessagesById
	// nextID store the ID of the next query
	nextID int
}

// NewHub returns a new Hub.
func NewHub(pubKeyOwner kyber.Point, clientServerAddress string, serverServerAddress string, log zerolog.Logger,
	laoFac channel.LaoFactory) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := Hub{
		clientServerAddress: clientServerAddress,
		serverServerAddress: serverServerAddress,
		messageChan:         make(chan socket.IncomingMessage),
		channelByID:         make(map[string]channel.Channel),
		closedSockets:       make(chan string),
		pubKeyOwner:         pubKeyOwner,
		pubKeyServ:          pubServ,
		secKeyServ:          secServ,
		schemaValidator:     schemaValidator,
		stop:                make(chan struct{}),
		workers:             semaphore.NewWeighted(numWorkers),
		log:                 log,
		laoFac:              laoFac,
		serverSockets:       channel.NewSockets(),
		hubInbox:            *inbox.NewInbox(rootChannel),
		rootInbox:           *inbox.NewInbox(rootChannel),
		queries:             newQueries(),
		messageIdsByChannel: make(map[string][]string),
		peersInfo:           make(map[string]method.ServerInfo),
		peersGreeted:        make([]string, 0),
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
				ok := h.workers.TryAcquire(1)
				if !ok {
					h.log.Warn().Msg("worker pool full, waiting...")
					h.workers.Acquire(context.Background(), 1)
				}

				go func() {
					err := h.handleIncomingMessage(&incomingMessage)
					if err != nil {
						h.log.Err(err).Msg("problem handling incoming message")
					}
				}()
			case id := <-h.closedSockets:
				h.RLock()
				for _, channel := range h.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				h.RUnlock()
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
	h.log.Info().Msg("waiting for existing workers to finish...")
	h.workers.Acquire(context.Background(), numWorkers)
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

// SendAndHandleMessage sends a publish message to all other known servers and
// handle it
func (h *Hub) SendAndHandleMessage(msg method.Broadcast) error {
	byteMsg, err := json.Marshal(msg)
	if err != nil {
		return xerrors.Errorf("failed to marshal publish message: %v", err)
	}

	h.log.Info().Str("msg", string(byteMsg)).Msg("sending new message")

	h.serverSockets.SendToAll(byteMsg)

	go func() {
		err = h.handleBroadcast(nil, byteMsg)
		if err != nil {
			h.log.Err(err).Msgf("Failed to handle self-produced message")
		}
	}()

	return nil
}

// OnSocketClose implements hub.Hub
func (h *Hub) OnSocketClose() chan<- string {
	return h.closedSockets
}

// SendGreetServer implements hub.Hub
func (h *Hub) SendGreetServer(socket socket.Socket) error {
	h.Lock()
	defer h.Unlock()

	pk, err := h.pubKeyServ.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal server public key: %v", err)
	}

	serverInfo := method.ServerInfo{
		PublicKey:     base64.URLEncoding.EncodeToString(pk),
		ServerAddress: h.serverServerAddress,
		ClientAddress: h.clientServerAddress,
	}

	serverGreet := &method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGreetServer,
		},
		Params: serverInfo,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		return xerrors.Errorf("failed to marshal server greet: %v", err)
	}

	socket.Send(buf)

	h.peersGreeted = append(h.peersGreeted, socket.ID())
	return nil
}

func (h *Hub) getChan(channelPath string) (channel.Channel, error) {
	if !strings.HasPrefix(channelPath, rootPrefix) {
		return nil, xerrors.Errorf("channel not prefixed with '%s': %q", rootPrefix, channelPath)
	}

	h.RLock()
	defer h.RUnlock()

	channel, ok := h.channelByID[channelPath]
	if !ok {
		return nil, xerrors.Errorf("channel %s does not exist", channelPath)
	}

	return channel, nil
}

// handleMessageFromClient handles an incoming message from an end user.
func (h *Hub) handleMessageFromClient(incomingMessage *socket.IncomingMessage) error {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		schemaErr := xerrors.Errorf("message is not valid against json schema: %v", err)
		socket.SendError(nil, schemaErr)
		return schemaErr
	}

	rpctype, err := jsonrpc.GetType(byteMessage)
	if err != nil {
		rpcErr := xerrors.Errorf("failed to get rpc type: %v", err)
		socket.SendError(nil, rpcErr)
		return rpcErr
	}

	if rpctype != jsonrpc.RPCTypeQuery {
		rpcErr := xerrors.New("rpc message sent by a client should be a query")
		socket.SendError(nil, rpcErr)
		return rpcErr
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		socket.SendError(nil, err)
		return err
	}

	var id int
	var msgs []message.Message
	var handlerErr error

	switch queryBase.Method {
	case query.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
		h.sendHeartbeatToServers()
	case query.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case query.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case query.MethodCatchUp:
		msgs, id, handlerErr = h.handleCatchup(socket, byteMessage)
	default:
		err = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		socket.SendError(&id, handlerErr)
		return err
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs, nil)
		return nil
	}

	socket.SendResult(id, nil, nil)

	return nil
}

// handleMessageFromServer handles an incoming message from a server.
func (h *Hub) handleMessageFromServer(incomingMessage *socket.IncomingMessage) error {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		schemaErr := xerrors.Errorf("message is not valid against json schema: %v", err)
		socket.SendError(nil, schemaErr)
		return schemaErr
	}

	rpctype, err := jsonrpc.GetType(byteMessage)
	if err != nil {
		rpcErr := xerrors.Errorf("failed to get rpc type: %v", err)
		socket.SendError(nil, rpcErr)
		return rpcErr
	}

	// check type (answer or query)
	if rpctype == jsonrpc.RPCTypeAnswer {
		err = h.handleAnswer(socket, byteMessage)
		if err != nil {
			err = answer.NewErrorf(-4, "failed to handle answer message: %v", err)
			socket.SendError(nil, err)
			return err
		}

		return nil
	}

	if rpctype != jsonrpc.RPCTypeQuery {
		rpcErr := xerrors.New("jsonRPC is of unknown type")
		socket.SendError(nil, rpcErr)
		return rpcErr
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		socket.SendError(nil, err)
		return err
	}

	var id int
	var msgsByChannel map[string][]message.Message
	var handlerErr error

	switch queryBase.Method {
	case query.MethodGreetServer:
		handlerErr = h.handleGreetServer(socket, byteMessage)
	case query.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
		h.sendHeartbeatToServers()
	case query.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case query.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case query.MethodHeartbeat:
		handlerErr = h.handleHeartbeat(socket, byteMessage)
	case query.MethodGetMessagesById:
		msgsByChannel, id, handlerErr = h.handleGetMessagesById(socket, byteMessage)

	default:
		err = answer.NewErrorf(-2, "unexpected method: '%s'", queryBase.Method)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		socket.SendError(&id, err)
		return err
	}

	if queryBase.Method == query.MethodGetMessagesById {
		socket.SendResult(id, nil, msgsByChannel)
		return nil
	}

	socket.SendResult(id, nil, nil)

	return nil
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *Hub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) error {
	defer h.workers.Release(1)

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

// sendGetMessagesByIdToServer sends a getMessagesById message to a server
func (h *Hub) sendGetMessagesByIdToServer(socket socket.Socket, missingIds map[string][]string) error {
	h.Lock()
	defer h.Unlock()

	queryId := h.queries.nextID
	baseValue := false
	h.queries.state[queryId] = &baseValue

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "get_messages_by_id",
		},
		ID:     queryId,
		Params: missingIds,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		return xerrors.Errorf("failed to marshal getMessagesById query: %v", err)
	}

	h.queries.getMessagesByIdQueries[queryId] = getMessagesById
	h.queries.nextID++

	socket.Send(buf)

	return nil
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() {
	h.Lock()
	defer h.Unlock()
	if len(h.messageIdsByChannel) > 0 {
		heartbeatMessage := method.Heartbeat{
			Base: query.Base{
				JSONRPCBase: jsonrpc.JSONRPCBase{
					JSONRPC: "2.0",
				},
				Method: "heartbeat",
			},
			Params: h.messageIdsByChannel,
		}

		buf, err := json.Marshal(heartbeatMessage)
		if err != nil {
			h.log.Err(err).Msg("Failed to marshal and send heartbeat query")
		}

		h.serverSockets.SendToAll(buf)
	}
}

// createLao creates a new LAO using the data in the publish parameter.
func (h *Hub) createLao(msg message.Message, laoCreate messagedata.LaoCreate,
	socket socket.Socket) error {

	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID[laoChannelPath]; ok {
		return answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	// Check if the sender of the LAO creation message is the organizer
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
	}

	organizerBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerBuf)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
	}

	// Check if the sender and organizer fields of the create lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the organizer field: %q != %q", senderPubKey, organizerPubKey)
	}

	// Check if the sender of the LAO creation message is the owner
	if h.GetPubKeyOwner() != nil && !h.GetPubKeyOwner().Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the owner's: %q != %q", senderPubKey, h.GetPubKeyOwner())
	}

	laoCh, err := h.laoFac(laoChannelPath, h, msg, h.log, senderPubKey, socket)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to create the LAO: %v", err)
	}

	h.log.Info().Msgf("storing new channel '%s' %v", laoChannelPath, msg)

	h.NotifyNewChannel(laoChannelPath, laoCh, socket)

	return nil
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
	h.Lock()
	h.channelByID[channelID] = channel
	h.Unlock()
}

// NotifyWitnessMessage implements channel.HubFunctionalities
func (h *Hub) NotifyWitnessMessage(messageId string, publicKey string, signature string) {
	h.Lock()
	h.hubInbox.AddWitnessSignature(messageId, publicKey, signature)
	h.Unlock()
}

func (h *Hub) GetPeersInfo() []method.ServerInfo {
	h.Lock()
	defer h.Unlock()

	var peersInfo []method.ServerInfo
	for _, info := range h.peersInfo {
		peersInfo = append(peersInfo, info)
	}

	return peersInfo
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

// addMessageId adds a message ID to the map of messageIds by channel of the hub
func (h *Hub) addMessageId(channelId string, messageId string) {
	messageIds, channelStored := h.messageIdsByChannel[channelId]
	if !channelStored {
		h.messageIdsByChannel[channelId] = append(h.messageIdsByChannel[channelId], messageId)
	} else {
		alreadyStored := slices.Contains(messageIds, messageId)
		if !alreadyStored {
			h.messageIdsByChannel[channelId] = append(h.messageIdsByChannel[channelId], messageId)
		}
	}
}
