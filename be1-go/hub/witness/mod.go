package witness

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math/rand"
	"popstellar/channel"
	"popstellar/hub"
	"popstellar/hub/serverInbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

// rootPrefix denotes the prefix for the root channel
// used to keep an image of the laos
const rootPrefix = "/root/"

// rpcUnknownError is an error message
const rpcUnknownError = "jsonRPC message is of unknown type"

// Strings used to return error messages
const jsonNotValidError = "message is not valid against json schema"
const unmarshalError = "failed to unmarshal incoming message %v"
const unexpectedMethodError = "unexpected method: '%s'"
const failedMethodHandling = "failed to handle method: %v"

const (
	numWorkers = 10
)

// Hub represents a Witness and implements the Hub interface.
type Hub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]channel.Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	// laoFac is there to allow a similar implementation to the organizer
	laoFac channel.LaoFactory

	serverSockets channel.Sockets

	inbox serverInbox.Inbox

	queriesID queriesID
}

type queriesID struct {
	mu sync.Mutex
	// queries store the ID of the server queries and their state. False for a
	// query not yet answered, else true.
	queries map[int]*bool
}

// NewHub returns a new Witness Hub.
func NewHub(public kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	witnessHub := Hub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		public:          public,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(numWorkers),
		log:             log,
		laoFac:          laoFac,
		serverSockets:   channel.NewSockets(),
		inbox:           *serverInbox.NewInbox("/root"),
		queriesID:       queriesID{queries: make(map[int]*bool)},
	}

	return &witnessHub, nil
}

// tempHandleMessage lets a witness handle message.
// As the Witness for now imitate the organizer when getting message, this
// function is there while waiting a correct witness implementation. Function
// there to reduce code duplication
func (h *Hub) tempHandleMessage(incMsg *socket.IncomingMessage) error {
	socket := incMsg.Socket
	byteMessage := incMsg.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg(jsonNotValidError)
		socket.SendError(nil, xerrors.Errorf(jsonNotValidError+": %v", err))
		return err
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, unmarshalError, err)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	rpctype, err := jsonrpc.GetType(byteMessage)
	if err != nil {
		h.log.Err(err).Msg("failed to get rpc type")
		rpcErr := xerrors.Errorf("failed to get rpc type: %v", err)
		socket.SendError(nil, rpcErr)
		return rpcErr
	}

	// check type (answer or query), we expect a query
	if rpctype == jsonrpc.RPCTypeAnswer {
		err = h.handleAnswer(byteMessage)
		if err != nil {
			return xerrors.Errorf("invalid answer message received: %v", err)
		}
	} else if rpctype != jsonrpc.RPCTypeQuery {
		h.log.Error().Msg(rpcUnknownError)
		rpcErr := xerrors.New(rpcUnknownError)
		socket.SendError(nil, rpcErr)
		return rpcErr
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
		err = answer.NewErrorf(-2, unexpectedMethodError, queryBase.Method)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, failedMethodHandling, handlerErr)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return err
	}

	socket.SendResult(id, nil)
	return nil
}

//
func (h *Hub) handleMessageFromOrganizer(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleMessageFromClient(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleMessageFromWitness(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleIncomingMessage(incMsg *socket.IncomingMessage) error {
	defer h.workers.Release(1)

	h.log.Info().Str("msg", fmt.Sprintf("%v", incMsg.Message)).
		Str("from", incMsg.Socket.ID()).
		Msgf("handle incoming message")

	switch incMsg.Socket.Type() {
	case socket.OrganizerSocketType:
		return h.handleMessageFromOrganizer(incMsg)
	case socket.ClientSocketType:
		return h.handleMessageFromClient(incMsg)
	case socket.WitnessSocketType:
		return h.handleMessageFromWitness(incMsg)
	default:
		h.log.Error().Msg("invalid socket type")
		return xerrors.Errorf("invalid socket type")
	}
}

func (h *Hub) handleAnswer(byteMessage []byte) error {
	var answer method.Answer

	err := json.Unmarshal(byteMessage, &answer)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal answer: %v", err)
	}

	h.queriesID.mu.Lock()

	val := h.queriesID.queries[answer.ID]
	if val == nil {
		h.queriesID.mu.Unlock()
		return xerrors.Errorf("no queries sent with this id")
	} else if *val {
		h.queriesID.mu.Unlock()
		return xerrors.Errorf("queries already got an answer")
	}

	*h.queriesID.queries[answer.ID] = true
	h.queriesID.mu.Unlock()

	for msg := range answer.Result {
		err := h.handleUnmarshaledPublish(answer.Result[msg])
		h.log.Err(err).Msg("failed to handle message during catchup")
	}
	return nil
}

func (h *Hub) handleUnmarshaledPublish(message method.Publish) error {
	if message.Params.Channel == "/root" {
		err := h.handleRootChannelPublishMesssage(nil, message)
		if err != nil {
			return xerrors.Errorf("failed to handle root channel message: %v", err)
		}
		return nil
	}

	channel, err := h.getChan(message.Params.Channel)
	if err != nil {
		return xerrors.Errorf("failed to get channel: %v", err)
	}

	err = channel.Publish(message)
	if err != nil {
		return xerrors.Errorf("failed to publish: %v", err)
	}

	return nil
}

// Start implements hub.Hub
func (h *Hub) Start() {
	h.log.Info().Msg("started witness...")

	go func() {
		for {
			select {
			case incMsg := <-h.messageChan:
				ok := h.workers.TryAcquire(1)
				if !ok {
					h.log.Warn().Msg("worker pool full, waiting...")
					h.workers.Acquire(context.Background(), 1)
				}

				err := h.handleIncomingMessage(&incMsg)
				if err != nil {
					h.log.Err(err).Msg("problem handling incoming message")
				}
			case id := <-h.closedSockets:
				h.RLock()
				for _, channel := range h.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				h.RUnlock()
			case <-h.stop:
				h.log.Info().Msg("closing the hub...")
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

// AddServerSocket adds a socket to the sockets known by the hub
func (h *Hub) AddServerSocket(socket socket.Socket) error {
	h.serverSockets.Upsert(socket)

	catchupID := h.generateID()

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
			"/root",
		},
	}

	buf, err := json.Marshal(rpcMessage)
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

// Type implements hub.Hub
func (h *Hub) Type() hub.HubType {
	return hub.WitnessHubType
}

// For now the witnesses work the same as organizer, all following functions are
// tied to this particular comportement.

// GetPubkey implements channel.HubFunctionalities
func (h *Hub) GetPubkey() kyber.Point {
	return h.public
}

// GetSchemaValidator implements channel.HubFunctionalities
func (h *Hub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

// generateID generates a unique ID in the hub used for queries from the server
func (h *Hub) generateID() int {
	h.queriesID.mu.Lock()
	defer h.queriesID.mu.Unlock()

	newID := 0
	for ok := false; !ok; _, ok = h.queriesID.queries[newID] {
		newID = rand.Int()
	}
	*h.queriesID.queries[newID] = false
	return newID
}

// broadcastToServers broadcast a message to all other known servers
func (h *Hub) broadcastToServers(message method.Publish, byteMessage []byte) {
	h.Lock()
	defer h.Unlock()
	_, ok := h.inbox.GetMessage(message.Params.Message.MessageID)
	if !ok {
		h.inbox.StoreMessage(message)
		h.serverSockets.SendToAll(byteMessage)
	}
}

// createLao creates a new LAO using the data in the publish parameter.
func (h *Hub) createLao(publish method.Publish, laoCreate messagedata.LaoCreate) error {

	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID[laoChannelPath]; ok {
		h.log.Error().Msgf("failed to create lao: duplicate lao path: %q", laoChannelPath)
		return answer.NewErrorf(-3, "failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	laoCh := h.laoFac(laoChannelPath, h, publish.Params.Message, h.log)

	h.log.Info().Msgf("storing new channel '%s' %v", laoChannelPath, publish.Params.Message)

	h.RegisterNewChannel(laoChannelPath, laoCh)

	return nil
}

// RegisterNewChannel implements channel.HubFunctionalities
func (h *Hub) RegisterNewChannel(channeID string, channel channel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// handleRootChannelPublishMesssage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelPublishMesssage(socket socket.Socket, publish method.Publish) error {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		socket.SendError(&publish.ID, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewErrorf(-1, "only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		h.log.Err(err)
		socket.SendError(&publish.ID, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		socket.SendError(&publish.ID, err)
		return err
	}

	err = h.createLao(publish, laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		socket.SendError(&publish.ID, err)
		return err
	}

	return nil
}

// handleRootChannelCatchupMessage handles an incoming catchup message on the root channel
func (h *Hub) handleRootChannelCatchupMessage(senderSocket socket.Socket, catchup method.Catchup) ([]method.Publish, error) {
	if senderSocket.Type() == socket.ClientSocketType {
		return nil, xerrors.Errorf("clients aren't allowed to send root channel catchup message")
	}
	messages := h.inbox.GetSortedMessages()
	return messages, nil
}

// handlePublish let a witness handle a publish message
func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	h.broadcastToServers(publish, byteMessage)

	if publish.Params.Channel == "/root" {
		err := h.handleRootChannelPublishMesssage(socket, publish)
		return publish.ID, err
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return -1, xerrors.Errorf("failed to get channel: %v", err)
	}

	err = channel.Publish(publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to publish: %v", err)
	}

	return publish.ID, nil
}

// handleSubscribe let a witness handle a subscribe message
func (h *Hub) handleSubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(byteMessage, &subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	channel, err := h.getChan(subscribe.Params.Channel)
	if err != nil {
		return -1, xerrors.Errorf("failed to get subscribe channel: %v", err)
	}

	err = channel.Subscribe(socket, subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to publish: %v", err)
	}

	return subscribe.ID, nil
}

// handleUnsubscribe let a witness handle an unsubscribe message
func (h *Hub) handleUnsubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(byteMessage, &unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	channel, err := h.getChan(unsubscribe.Params.Channel)
	if err != nil {
		return -1, xerrors.Errorf("failed to get unsubscribe channel: %v", err)
	}

	err = channel.Unsubscribe(socket.ID(), unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unsubscribe: %v", err)
	}

	return unsubscribe.ID, nil
}

// handleCatchup let a witness handle a catchup message
func (h *Hub) handleCatchup(socket socket.Socket, byteMessage []byte) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel == "/root" {
		messages, err := h.handleRootChannelCatchupMessage(socket, catchup)
		if err != nil {
			return nil, catchup.ID, xerrors.Errorf("failed to handle root channel catchup message: %v", err)
		}
		h.sendAnswerToServer(socket, catchup.ID, messages)
		return nil, catchup.ID, nil
	}

	channel, err := h.getChan(catchup.Params.Channel)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to get catchup channel: %v", err)
	}

	msg := channel.Catchup(catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to catchup: %v", err)
	}

	return msg, catchup.ID, nil
}

func (h *Hub) sendAnswerToServer(socket socket.Socket, id int, messages []method.Publish) {
	socket.SendServerResult(id, messages)
}

// getChan finds a channel based on its path
func (h *Hub) getChan(channelPath string) (channel.Channel, error) {
	if channelPath[:6] != rootPrefix {
		return nil, xerrors.Errorf("channel id must begin with \"/root/\", got: %q", channelPath[:6])
	}

	h.RLock()
	defer h.RUnlock()

	channel, ok := h.channelByID[channelPath]
	if !ok {
		return nil, xerrors.Errorf("channel %s does not exist", channelPath)
	}

	return channel, nil
}
