package standard_hub

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/answer/manswer"
	"popstellar/internal/handler/channel/root/mroot"
	jsonrpc "popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	method2 "popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/hub/standard_hub/hub_state"
	"popstellar/internal/old/inbox"
	"popstellar/internal/old/oldchannel"
	"popstellar/internal/validation"
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
	// rootChannel denotes the id of the root oldchannel
	rootChannel = "/root"

	// rootPrefix denotes the prefix for the root oldchannel
	// used to keep an image of the laos
	rootPrefix = rootChannel + "/"

	// Strings used to return error messages
	rootChannelErr = "failed to handle root oldchannel message: %v"
	getChannelErr  = "failed to get oldchannel: %v"

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
	channelByID hub_state.Channels

	closedSockets chan string

	pubKeyOwner kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac oldchannel.LaoFactory

	serverSockets oldchannel.Sockets

	// hubInbox is used to remember the messages that the hub received
	hubInbox inbox.HubInbox

	// queries are used to help servers catchup to each other
	queries hub_state.Queries

	// peers stores information about the peers
	peers hub_state.Peers

	// blacklist stores the IDs of the messages that failed to be processed by the hub
	// the server will not ask for them again in the heartbeat
	// and will not process them if they are received again
	// @TODO remove the messages from the blacklist after a certain amount of time by trying to process them again
	blacklist hub_state.ThreadSafeSlice[string]
}

// NewHub returns a new Hub.
func NewHub(pubKeyOwner kyber.Point, clientServerAddress string, serverServerAddress string, log zerolog.Logger,
	laoFac oldchannel.LaoFactory,
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
		messageChan:         make(chan socket.IncomingMessage),
		channelByID:         hub_state.NewChannelsMap(),
		closedSockets:       make(chan string),
		pubKeyOwner:         pubKeyOwner,
		pubKeyServ:          pubServ,
		secKeyServ:          secServ,
		schemaValidator:     schemaValidator,
		stop:                make(chan struct{}),
		workers:             semaphore.NewWeighted(numWorkers),
		log:                 log,
		laoFac:              laoFac,
		serverSockets:       oldchannel.NewSockets(),
		hubInbox:            *inbox.NewHubInbox(rootChannel),
		queries:             hub_state.NewQueries(log),
		peers:               hub_state.NewPeers(),
		blacklist:           hub_state.NewThreadSafeSlice[string](),
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
				h.channelByID.ForEach(func(c oldchannel.Channel) {
					// dummy Unsubscribe message because it's only used for logging...
					c.Unsubscribe(id, method2.Unsubscribe{})
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
func (h *Hub) SendAndHandleMessage(msg mbroadcast.Broadcast) error {
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
	pk, err := h.pubKeyServ.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal server public key: %v", err)
	}

	serverInfo := mgreetserver.GreetServerParams{
		PublicKey:     base64.URLEncoding.EncodeToString(pk),
		ServerAddress: h.serverServerAddress,
		ClientAddress: h.clientServerAddress,
	}

	serverGreet := &mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		return xerrors.Errorf("failed to marshal server greet: %v", err)
	}

	socket.Send(buf)

	h.peers.AddPeerGreeted(socket.ID())
	return nil
}

func (h *Hub) getChan(channelPath string) (oldchannel.Channel, error) {
	if !strings.HasPrefix(channelPath, rootPrefix) {
		return nil, xerrors.Errorf("oldchannel not prefixed with '%s': %q", rootPrefix, channelPath)
	}

	channel, ok := h.channelByID.Get(channelPath)
	if !ok {
		return nil, xerrors.Errorf("oldchannel %s does not exist", channelPath)
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

	var queryBase mquery.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := manswer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		socket.SendError(nil, err)
		return err
	}

	var id int
	var msgs []mmessage.Message
	var handlerErr error

	switch queryBase.Method {
	case mquery.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
		h.sendHeartbeatToServers()
	case mquery.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case mquery.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case mquery.MethodCatchUp:
		msgs, id, handlerErr = h.handleCatchup(socket, byteMessage)
	default:
		err = manswer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		socket.SendError(&id, handlerErr)
		return err
	}

	if queryBase.Method == mquery.MethodCatchUp {
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
			err = manswer.NewErrorf(-4, "failed to handle answer message: %v", err)
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

	var queryBase mquery.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := manswer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		socket.SendError(nil, err)
		return err
	}

	id := -1
	var msgsByChannel map[string][]mmessage.Message
	var handlerErr error

	switch queryBase.Method {
	case mquery.MethodGreetServer:
		handlerErr = h.handleGreetServer(socket, byteMessage)
	case mquery.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
		h.sendHeartbeatToServers()
	case mquery.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case mquery.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case mquery.MethodHeartbeat:
		handlerErr = h.handleHeartbeat(socket, byteMessage)
	case mquery.MethodGetMessagesById:
		msgsByChannel, id, handlerErr = h.handleGetMessagesById(socket, byteMessage)

	default:
		err = manswer.NewErrorf(-2, "unexpected method: '%s'", queryBase.Method)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		err := manswer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		socket.SendError(&id, err)
		return err
	}

	if queryBase.Method == mquery.MethodGetMessagesById {
		socket.SendResult(id, nil, msgsByChannel)
		return nil
	}

	if id != -1 {
		socket.SendResult(id, nil, nil)
	}

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
	queryId := h.queries.GetNextID()

	getMessagesById := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
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

	socket.Send(buf)

	h.queries.AddQuery(queryId, getMessagesById)

	return nil
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() {
	heartbeatMessage := mheartbeat.Heartbeat{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "heartbeat",
		},
		Params: h.hubInbox.GetIDsTable(),
	}

	buf, err := json.Marshal(heartbeatMessage)
	if err != nil {
		h.log.Err(err).Msg("Failed to marshal and send heartbeat query")
	}
	h.serverSockets.SendToAll(buf)
}

// createLao creates a new LAO using the data in the publish parameter.
func (h *Hub) createLao(msg mmessage.Message, laoCreate mroot.LaoCreate,
	socket socket.Socket,
) error {
	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID.Get(laoChannelPath); ok {
		return manswer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return manswer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	// Check if the sender of the LAO creation message is the organizer
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return manswer.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
	}

	organizerBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		return manswer.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerBuf)
	if err != nil {
		return manswer.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
	}

	// Check if the sender and organizer fields of the create lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		return manswer.NewAccessDeniedError("sender's public key does not match the organizer field: %q != %q", senderPubKey, organizerPubKey)
	}

	// Check if the sender of the LAO creation message is the owner
	if h.GetPubKeyOwner() != nil && !h.GetPubKeyOwner().Equal(senderPubKey) {
		return manswer.NewAccessDeniedError("sender's public key does not match the owner's: %q != %q", senderPubKey, h.GetPubKeyOwner())
	}

	laoCh, err := h.laoFac(laoChannelPath, h, msg, h.log, senderPubKey, socket)
	if err != nil {
		return manswer.NewInvalidMessageFieldError("failed to create the LAO: %v", err)
	}

	h.log.Info().Msgf("storing new oldchannel '%s' %v", laoChannelPath, msg)

	h.NotifyNewChannel(laoChannelPath, laoCh, socket)

	return nil
}

// GetPubKeyOwner implements oldchannel.HubFunctionalities
func (h *Hub) GetPubKeyOwner() kyber.Point {
	return h.pubKeyOwner
}

// GetPubKeyServ implements oldchannel.HubFunctionalities
func (h *Hub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
}

// GetClientServerAddress implements oldchannel.HubFunctionalities
func (h *Hub) GetClientServerAddress() string {
	return h.clientServerAddress
}

// Sign implements oldchannel.HubFunctionalities
func (h *Hub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}

	return signatureBuf, nil
}

// GetSchemaValidator implements oldchannel.HubFunctionalities
func (h *Hub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

// NotifyNewChannel implements oldchannel.HubFunctionalities
func (h *Hub) NotifyNewChannel(channelID string, channel oldchannel.Channel, sock socket.Socket) {
	h.channelByID.Set(channelID, channel)
}

// NotifyWitnessMessage implements oldchannel.HubFunctionalities
func (h *Hub) NotifyWitnessMessage(messageId string, publicKey string, signature string) {
	h.hubInbox.AddWitnessSignature(messageId, publicKey, signature)
}

func (h *Hub) GetPeersInfo() []mgreetserver.GreetServerParams {
	return h.peers.GetAllPeersInfo()
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}
