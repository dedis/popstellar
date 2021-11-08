package organizer

import (
	"context"
	"database/sql"
	"encoding/base64"
	"encoding/json"
	be1_go "popstellar"
	"popstellar/channel"
	"popstellar/channel/lao"
	"popstellar/db/sqlite"
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
const rootPrefix = "/root/"

// serverComChannel denotes the channel used to communicate between servers
const serverComChannel = "/root/serverCom"

// rpcUnknownError is an error message
const rpcUnknownError = "jsonRPC message is of unknown type"

const (
	// numWorkers denote the number of worker go-routines
	// allowed to process requests concurrently.
	numWorkers = 10

	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
)

// Hub implements the Hub interface.
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

	laoFac channel.LaoFactory

	serverSockets channel.Sockets

	inbox serverInbox.Inbox

	nextID int

	queriesID queriesID
}

type queriesID struct {
	mu sync.Mutex
	// queries store the ID of the server queries and their state. False for a
	// query not yet answered, else true.
	queries map[int]*bool
}

// NewHub returns a Organizer Hub.
func NewHub(public kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	hub := Hub{
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
		nextID:          0,
		queriesID:       queriesID{queries: make(map[int]*bool)},
	}

	if sqlite.GetDBPath() != "" {
		log.Info().Msgf("loading channels from db at %s", sqlite.GetDBPath())

		channels, err := getChannelsFromDB(&hub)
		if err != nil {
			log.Err(err).Msg("failed to load channels from db")
		} else {
			hub.channelByID = channels
		}
	}

	return &hub, nil
}

// Type implements hub.Hub
func (h *Hub) Type() hub.HubType {
	return hub.OrganizerHubType
}

// Start implements hub.Hub
func (h *Hub) Start() {
	go func() {
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

// AddServerSocket adds a socket to the sockets known by the hub and query a
// catchup from the new server
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
			serverComChannel,
		},
	}

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

// generateID generates a unique ID in the hub used for queries from the server
func (h *Hub) generateID() int {
	h.queriesID.mu.Lock()
	defer h.queriesID.mu.Unlock()

	newID := h.nextID
	h.nextID++

	//	*h.queriesID.queries[newID] = false
	return newID
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
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		socket.SendError(&publish.ID, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
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

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		socket.SendError(&publish.ID, err)
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
		err := xerrors.Errorf("clients aren't allowed to send root channel catchup message")
		return nil, err
	}
	messages := h.inbox.GetSortedMessages()
	return messages, nil
}

// handleMessageFromClient handles an incoming message from an end user.
func (h *Hub) handleMessageFromClient(incomingMessage *socket.IncomingMessage) error {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg("message is not valid against json schema")
		schemaErr := xerrors.Errorf("message is not valid against json schema: %v", err)
		socket.SendError(nil, schemaErr)
		return schemaErr
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

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		h.log.Err(err)
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
		err = answer.NewErrorf(-2, "unexpected method: '%s'", queryBase.Method)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return nil
	}

	socket.SendResult(id, nil)
	return nil
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

func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	h.broadcastToServers(publish, byteMessage)

	if publish.Params.Channel == "/root" {
		err := h.handleRootChannelPublishMesssage(socket, publish)
		if err != nil {
			return -1, xerrors.Errorf("failed to handle root channel message: %v", err)
		}
		return publish.ID, nil
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

func (h *Hub) handleCatchup(socket socket.Socket, byteMessage []byte) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel == serverComChannel {
		messages, err := h.handleRootChannelCatchupMessage(socket, catchup)
		if err != nil {
			return nil, catchup.ID, xerrors.Errorf("failed to handle root channel catchup message: %v", err)
		}
		socket.SendServerResult(catchup.ID, messages)
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

// handleMessageFromWitness handles an incoming message from a witness server.
// this may change once the witness are correctly implemented
func (h *Hub) handleMessageFromWitness(incomingMessage *socket.IncomingMessage) error {
	// With the simplified comportement of the witness, the message should be
	// handled same way as a client message
	return h.handleMessageFromClient(incomingMessage)
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *Hub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) error {
	defer h.workers.Release(1)

	h.log.Info().Str("msg", string(incomingMessage.Message)).Msg("handle incoming message")

	switch incomingMessage.Socket.Type() {
	case socket.ClientSocketType:
		return h.handleMessageFromClient(incomingMessage)
	case socket.WitnessSocketType:
		return h.handleMessageFromWitness(incomingMessage)
	default:
		h.log.Error().Msg("invalid socket type")
		return xerrors.Errorf("invalid socket type")
	}

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

	if sqlite.GetDBPath() != "" {
		saveChannel(laoChannelPath)
	}

	return nil
}

// GetPubkey implements channel.HubFunctionalities
func (h *Hub) GetPubkey() kyber.Point {
	return h.public
}

// GetSchemaValidator implements channel.HubFunctionalities
func (h *Hub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

// RegisterNewChannel implements channel.HubFunctionalities
func (h *Hub) RegisterNewChannel(channeID string, channel channel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// ---
// DB operations
// --

func saveChannel(channelPath string) error {
	log := be1_go.Logger

	log.Info().Msgf("trying to save the channel in db at %s", sqlite.GetDBPath())

	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
	INSERT INTO
		lao_channel(
			lao_channel_id) 
	VALUES(?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(channelPath)
	if err != nil {
		log.Err(err).Msg("failed to save channel in db")
		return xerrors.Errorf("failed to insert channel: %v", err)
	}

	return nil
}

func getChannelsFromDB(h *Hub) (map[string]channel.Channel, error) {
	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return nil, xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
		SELECT
			lao_channel_id
		FROM
			lao_channel`

	rows, err := db.Query(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to query channels: %v", err)
	}

	defer rows.Close()

	result := make(map[string]channel.Channel)

	for rows.Next() {
		var channelPath string

		err = rows.Scan(&channelPath)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		channel, err := lao.CreateChannelFromDB(db, channelPath, h, h.log)
		if err != nil {
			return nil, xerrors.Errorf("failed to create channel from db: %v", err)
		}

		result[channelPath] = channel
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
