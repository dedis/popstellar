package hub

import (
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	answerHandler "popstellar/internal/handler/answer"
	"popstellar/internal/handler/jsonrpc"
	messageHandler "popstellar/internal/handler/message"
	"popstellar/internal/handler/messagedata/chirp"
	"popstellar/internal/handler/messagedata/coin"
	"popstellar/internal/handler/messagedata/election"
	"popstellar/internal/handler/messagedata/federation"
	"popstellar/internal/handler/messagedata/lao"
	"popstellar/internal/handler/messagedata/reaction"
	"popstellar/internal/handler/messagedata/root"
	"popstellar/internal/handler/method/catchup"
	"popstellar/internal/handler/method/getmessagesbyid"
	"popstellar/internal/handler/method/greetserver"
	"popstellar/internal/handler/method/heartbeat"
	"popstellar/internal/handler/method/publish"
	"popstellar/internal/handler/method/rumor"
	"popstellar/internal/handler/method/subscribe"
	"popstellar/internal/handler/method/unsubscribe"
	queryHandler "popstellar/internal/handler/query"
	"popstellar/internal/logger"
	"popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/repository"
	"popstellar/internal/sqlite"
	"popstellar/internal/types"
	"popstellar/internal/validation"
	"time"
)

const (
	heartbeatDelay = time.Second * 30
	rumorDelay     = time.Second * 5
)

type JsonRpcHandler interface {
	Handle(socket socket.Socket, msg []byte) error
}

type GreetServerSender interface {
	SendGreetServer(socket socket.Socket) error
}

type RumorSender interface {
	SendRumor(socket socket.Socket, rumor method.Rumor)
}

type Hub struct {
	// in memory states
	conf    repository.ConfigManager
	control repository.HubManager
	subs    repository.SubscriptionManager
	sockets repository.SocketManager

	// database
	db repository.HubRepository

	// handlers
	jsonRpcHandler    JsonRpcHandler
	greetserverSender GreetServerSender
	rumorSender       RumorSender
}

func New(dbPath string, ownerPubKey kyber.Point, clientAddress, serverAddress string) (*Hub, error) {

	// Initialize the in memory states
	subs := types.NewSubscribers()
	peers := types.NewPeers()
	queries := types.NewQueries(&logger.Logger)
	hubParams := types.NewHubParams()
	sockets := types.NewSockets()

	// Initialize the database
	db, err := sqlite.NewSQLite(dbPath, true)
	if err != nil {
		return nil, err
	}

	// Get the server keys from the database
	serverPublicKey, serverSecretKey, err := db.GetServerKeys()
	if err != nil {
		// If the server keys are not found, generate new ones
		serverSecretKey = crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
		serverPublicKey = crypto.Suite.Point().Mul(serverSecretKey, nil)

		err := db.StoreServerKeys(serverPublicKey, serverSecretKey)
		if err != nil {
			return nil, err
		}
	}

	// Create the in memory configuration state
	conf := types.CreateConfig(ownerPubKey, serverPublicKey, serverSecretKey, clientAddress, serverAddress)

	// Store the rumor with id 0 associated to the server if the database is empty
	err = db.StoreFirstRumor()
	if err != nil {
		return nil, err
	}

	// Get all the channels from the database and add them to the subscribers
	channels, err := db.GetAllChannels()
	if err != nil {
		return nil, err
	}

	for _, channel := range channels {
		alreadyExist := subs.HasChannel(channel)
		if alreadyExist {
			continue
		}

		err = subs.AddChannel(channel)
		if err != nil {
			return nil, err
		}
	}

	// Create the schema validator
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, err
	}

	// Create the message data handlers
	dataHandlers := messageHandler.MessageDataHandlers{
		Root:       root.New(conf, &db, subs, peers, schemaValidator),
		Lao:        lao.New(conf, subs, &db, schemaValidator),
		Election:   election.New(conf, subs, &db, schemaValidator),
		Chirp:      chirp.New(conf, subs, &db, schemaValidator),
		Reaction:   reaction.New(subs, &db, schemaValidator),
		Coin:       coin.New(subs, &db, schemaValidator),
		Federation: federation.New(&db, subs, sockets, hubParams, schemaValidator),
	}

	// Create the message handler
	msgHandler := messageHandler.New(&db, dataHandlers)

	// Create the greetserver handler
	greetserverHandler := greetserver.New(conf, peers)

	// Create the rumor handler
	rumorHandler := rumor.New(queries, sockets, &db, msgHandler)

	// Create the query handler
	qHandler := queryHandler.New(queryHandler.MethodHandlers{
		Catchup:         catchup.New(&db),
		GetMessagesbyid: getmessagesbyid.New(&db),
		Greetserver:     greetserverHandler,
		Heartbeat:       heartbeat.New(queries, &db),
		Publish:         publish.New(hubParams, &db, msgHandler),
		Subscribe:       subscribe.New(subs),
		Unsubscribe:     unsubscribe.New(subs),
		Rumor:           rumorHandler,
	})

	// Create the answer handler
	aHandler := answerHandler.New(queries, answerHandler.AnswerHandlers{
		MessageHandler: msgHandler,
		RumorSender:    rumorHandler,
	})

	// Create the json rpc handler
	jsonRpcHandler := jsonrpc.New(schemaValidator, qHandler, aHandler)

	// Create the hub
	hub := &Hub{
		conf:              conf,
		control:           hubParams,
		subs:              subs,
		sockets:           sockets,
		jsonRpcHandler:    jsonRpcHandler,
		greetserverSender: greetserverHandler,
		rumorSender:       rumorHandler,
		db:                &db,
	}

	return hub, nil
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	h.sockets.Upsert(socket)
}

func (h *Hub) Start() {
	h.control.GetWaitGroup().Add(3)
	go h.runHeartbeat()
	go h.runRumorSender()
	go h.runMessageReceiver()
}

func (h *Hub) Stop() {
	close(h.control.GetStopChan())
	h.control.GetWaitGroup().Wait()
}

func (h *Hub) Receiver() chan<- socket.IncomingMessage {
	return h.control.GetMessageChan()
}

func (h *Hub) OnSocketClose() chan<- string {
	return h.control.GetClosedSockets()
}

func (h *Hub) SendGreetServer(socket socket.Socket) error {
	return h.greetserverSender.SendGreetServer(socket)
}

func (h *Hub) runMessageReceiver() {
	defer h.control.GetWaitGroup().Done()
	defer logger.Logger.Info().Msg("stopping hub")

	logger.Logger.Info().Msg("starting hub")

	for {
		logger.Logger.Debug().Msg("waiting for a new message")
		select {
		case incomingMessage := <-h.control.GetMessageChan():
			logger.Logger.Debug().Msg("start handling a message")
			err := h.jsonRpcHandler.Handle(incomingMessage.Socket, incomingMessage.Message)
			if err != nil {
				logger.Logger.Error().Err(err)
			} else {
				logger.Logger.Debug().Msg("successfully handled a message")
			}
		case socketID := <-h.control.GetClosedSockets():
			logger.Logger.Debug().Msgf("stopping the Socket " + socketID)
			h.subs.UnsubscribeFromAll(socketID)
		case <-h.control.GetStopChan():
			return
		}
	}
}

func (h *Hub) runRumorSender() {
	ticker := time.NewTicker(rumorDelay)
	defer ticker.Stop()
	defer h.control.GetWaitGroup().Done()
	defer logger.Logger.Info().Msg("stopping rumor sender")

	logger.Logger.Info().Msg("starting rumor sender")

	for {
		select {
		case <-ticker.C:
			logger.Logger.Debug().Msgf("sender rumor trigerred")
			err := h.tryToSendRumor()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.control.GetResetRumorSender():
			logger.Logger.Debug().Msgf("sender rumor reset")
			ticker.Reset(rumorDelay)
			err := h.tryToSendRumor()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.control.GetStopChan():
			return
		}
	}
}

func (h *Hub) tryToSendRumor() error {
	ok, r, err := h.db.GetAndIncrementMyRumor()
	if err != nil {
		return err
	}
	if !ok {
		logger.Logger.Debug().Msg("no new rumor to send")
		return nil
	}

	h.rumorSender.SendRumor(nil, r)

	return nil
}

func (h *Hub) runHeartbeat() {
	ticker := time.NewTicker(heartbeatDelay)
	defer ticker.Stop()
	defer h.control.GetWaitGroup().Done()
	defer logger.Logger.Info().Msg("stopping heartbeat sender")

	logger.Logger.Info().Msg("starting heartbeat sender")

	for {
		select {
		case <-ticker.C:
			err := h.sendHeartbeat()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.control.GetStopChan():
			return
		}
	}
}

// sendHeartbeat sends a heartbeat message to all servers
func (h *Hub) sendHeartbeat() error {
	heartbeatMessage := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: message.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "heartbeat",
		},
		Params: make(map[string][]string),
	}

	buf, err := json.Marshal(heartbeatMessage)
	if err != nil {
		return errors.NewJsonMarshalError(err.Error())
	}

	h.sockets.SendToAll(buf)

	return nil
}
