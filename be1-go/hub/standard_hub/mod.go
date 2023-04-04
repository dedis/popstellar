package standard_hub

import (
	"context"
	"encoding/base64"
	"encoding/json"
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
	heartbeatDelay = 7
)

var suite = crypto.Suite

// Hub implements the Hub interface.
type Hub struct {
	serverAdress string

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

	// hubInbox is used to remember which messages were broadcast by the
	// server to avoid broadcast loops
	hubInbox inbox.Inbox

	// rootInbox and queries are used to help servers catchup to each other
	rootInbox inbox.Inbox
	queries   queries

	// messageIdsByChannel stores all the message ids and the corresponding channel ids
	// to help servers
	messageIdsByChannel map[string][]string
}

// newQueries creates a new queries struct
func newQueries() queries {
	return queries{
		state:                  make(map[int]*bool),
		catchupQueries:         make(map[int]method.Catchup),
		getMessagesByIdQueries: make(map[int]method.GetMessagesById),
	}
}

// queries let the hub remember all queries that it sent to other servers
type queries struct {
	sync.Mutex
	// state stores the ID of the server's queries and their state. False for a
	// query not yet answered, else true.
	state map[int]*bool
	// catchupQueries stores the server's catchup queries by their ID.
	catchupQueries map[int]method.Catchup
	// getMessagesByIdQueries stores the server's getMessagesByIds queries by their ID.
	getMessagesByIdQueries map[int]method.GetMessagesById
	// nextID store the ID of the next query
	nextID int
}

func (q *queries) getNextCatchupMessage(channel string) method.Catchup {
	q.Lock()
	defer q.Unlock()

	catchupID := q.nextID

	baseValue := false
	q.state[catchupID] = &baseValue

	rpcMessage := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "catchup",
		},
		ID: catchupID,
		Params: struct {
			Channel string "json:\"channel\""
		}{
			channel,
		},
	}

	q.catchupQueries[catchupID] = rpcMessage
	q.nextID++

	return rpcMessage
}

// NewHub returns a new Hub.
func NewHub(pubKeyOwner kyber.Point, serverAddress string, log zerolog.Logger,
	laoFac channel.LaoFactory) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := Hub{
		serverAdress:        serverAddress,
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
	}

	return &hub, nil
}

// Start implements hub.Hub
func (h *Hub) Start() {
	go func() {
		ticker := time.NewTicker(time.Second * heartbeatDelay)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				err := h.sendHeartbeatToServers()
				if err != nil {
					h.log.Err(err).Msg("problem sending heartbeat to servers")
				}
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

// NotifyNewServer adds a socket to the sockets known by the hub and query a
// catchup from the new server
func (h *Hub) NotifyNewServer(socket socket.Socket) error {
	h.serverSockets.Upsert(socket)

	err := h.catchupToServer(socket, rootChannel)
	return err
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

// catchupToServer sends a catchup query to another server
func (h *Hub) catchupToServer(socket socket.Socket, channel string) error {

	rpcMessage := h.queries.getNextCatchupMessage(channel)

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("server failed to marshal catchup query: %v", err)
	}

	socket.Send(buf)

	return nil
}

// OnSocketClose implements hub.Hub
func (h *Hub) OnSocketClose() chan<- string {
	return h.closedSockets
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
	var msgs []message.Message
	var msgsByChannel map[string][]message.Message
	var handlerErr error

	switch queryBase.Method {
	case query.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
	case query.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case query.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case query.MethodCatchUp:
		msgs, id, handlerErr = h.handleCatchup(socket, byteMessage)
	case query.MethodBroadcast:
		handlerErr = h.handleBroadcast(socket, byteMessage)
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

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs, nil)
		return nil
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

func (h *Hub) sendGetMessagesByIdToServer(socket socket.Socket, missingIds map[string][]string) error {
	h.Lock()
	defer h.Unlock()
	h.log.Info().Msg("Entering getMessagesById")

	queryId := h.queries.nextID
	baseValue := false
	h.queries.state[queryId] = &baseValue

	h.log.Info().Msg("Sending getMessagesById")
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

// sendHeartbeatToServers send a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() error {
	h.Lock()
	defer h.Unlock()
	h.log.Info().Msg("Entering heartbeat")

	if len(h.messageIdsByChannel) >= 1 {
		h.log.Info().Msg("Sending heartbeat")
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
			return xerrors.Errorf("failed to marshal heartbeat query: %v", err)
		}

		h.serverSockets.SendToAll(buf)
	}
	return nil
}

// broadcastToServers broadcast a message to all other known servers
func (h *Hub) broadcastToServers(msg message.Message, channel string) (bool, error) {
	h.Lock()
	defer h.Unlock()

	_, ok := h.hubInbox.GetMessage(msg.MessageID)
	if ok {
		return true, nil
	}

	broadcastMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			channel,
			msg,
		},
	}

	buf, err := json.Marshal(broadcastMessage)
	if err != nil {
		return false, xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	h.serverSockets.SendToAll(buf)
	h.hubInbox.StoreMessage(msg)

	h.messageIdsByChannel[channel] = append(h.messageIdsByChannel[channel], msg.MessageID)

	return false, nil
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

	if h.GetPubKeyOwner() != nil && !h.GetPubKeyOwner().Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the organizer's: %q != %q",
			senderPubKey, h.GetPubKeyOwner())
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

// GetServerAddress implements channel.HubFunctionalities
func (h *Hub) GetServerAddress() string {
	return h.serverAdress
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

	if sock.Type() == socket.ServerSocketType {
		h.log.Info().Msgf("catching up on channel %v", channelID)
		h.catchupToServer(sock, channelID)
	}
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}
