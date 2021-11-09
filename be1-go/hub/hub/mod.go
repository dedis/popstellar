package hub

import (
	"context"
	"database/sql"
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

const (
	// rootPrefix denotes the prefix for the root channel
	// used to keep an image of the laos
	rootPrefix = "/root/"

	// serverComChannel denotes the channel used to communicate between servers
	serverComChannel = "/root/serverCom"

	// Strings used to return error messages in relation with a database
	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"

	numWorkers = 10
)

// Hub implements the Hub interface.
type Hub struct {
	hubType hub.HubType

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

	queries queries
}

type queries struct {
	mu sync.Mutex
	// queries store the ID of the server queries and their state. False for a
	// query not yet answered, else true.
	queries map[int]*bool

	// nextID store the ID of the next query
	nextID int
}

// NewHub returns a new Witness Hub.
func NewHub(public kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory, hubType hub.HubType) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	hub := Hub{
		hubType:         hubType,
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
		queries:         queries{queries: make(map[int]*bool), nextID: 0},
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
	return h.hubType
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
	h.queries.mu.Lock()
	defer h.queries.mu.Unlock()

	newID := h.queries.nextID
	h.queries.nextID += 1

	baseValue := false
	h.queries.queries[newID] = &baseValue
	return newID
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

	if rpctype != jsonrpc.RPCTypeQuery {
		h.log.Error().Msg("rpc message sent by a client should be a query")
		rpcErr := xerrors.New("rpc message sent by a client should be a query")
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

// handleMessageFromServer handles an incoming message from a witness server.
func (h *Hub) handleMessageFromServer(incomingMessage *socket.IncomingMessage) error {
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
		err = h.handleAnswer(socket, byteMessage)
		if err != nil {
			return xerrors.Errorf("failed to handle answer message: %v", err)
		}
		return nil
	} else if rpctype != jsonrpc.RPCTypeQuery {
		h.log.Error().Msg("jsonRPC is of unknown type")
		rpcErr := xerrors.New("jsonRPC is of unknown type")
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
	var serverMsgs []string
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
		if msgs == nil && handlerErr == nil {
			serverMsgs, id, handlerErr = h.handleServerCatchup(socket, byteMessage)
		}
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

	if serverMsgs != nil {
		socket.SendServerResult(id, serverMsgs)
		return nil
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return nil
	}

	socket.SendResult(id, nil)
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
	case socket.WitnessSocketType:
		return h.handleMessageFromServer(incomingMessage)
	case socket.OrganizerSocketType:
		return h.handleMessageFromServer(incomingMessage)
	default:
		h.log.Error().Msg("invalid socket type")
		return xerrors.Errorf("invalid socket type")
	}

}

// broadcastToServers broadcast a message to all other known servers
func (h *Hub) broadcastToServers(message method.Publish, byteMessage []byte) bool {
	h.Lock()
	defer h.Unlock()
	_, ok := h.inbox.GetMessage(message.Params.Message.MessageID)
	if !ok {
		h.inbox.StoreMessage(message)
		h.serverSockets.SendToAll(byteMessage)
		return false
	}
	return true
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
