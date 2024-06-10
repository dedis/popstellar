package hub

import (
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/database/sqlite"
	"popstellar/internal/errors"
	"popstellar/internal/handler/answer/hanswer"
	"popstellar/internal/handler/jsonrpc/hjsonrpc"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/hmessage"
	"popstellar/internal/handler/messagedata/chirp/hchirp"
	"popstellar/internal/handler/messagedata/coin/hcoin"
	"popstellar/internal/handler/messagedata/election/helection"
	"popstellar/internal/handler/messagedata/federation/hfederation"
	"popstellar/internal/handler/messagedata/lao/hlao"
	"popstellar/internal/handler/messagedata/reaction/hreaction"
	"popstellar/internal/handler/messagedata/root/hroot"
	"popstellar/internal/handler/method/catchup/hcatchup"
	"popstellar/internal/handler/method/getmessagesbyid/hgetmessagesbyid"
	"popstellar/internal/handler/method/greetserver/hgreetserver"
	"popstellar/internal/handler/method/heartbeat/hheartbeat"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	"popstellar/internal/handler/method/publish/hpublish"
	"popstellar/internal/handler/method/rumor/hrumor"
	method2 "popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/subscribe/hsubscribe"
	"popstellar/internal/handler/method/unsubscribe/hunsubscribe"
	"popstellar/internal/handler/query/hquery"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/logger"
	"popstellar/internal/network/socket"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"sync"
	"time"
)

const (
	heartbeatDelay = time.Second * 30
	rumorDelay     = time.Second * 5
)

type Subscribers interface {
	HasChannel(channelPath string) bool
	AddChannel(channelPath string) error
	UnsubscribeFromAll(socketID string)
}

type Sockets interface {
	Upsert(socket socket.Socket)
	SendToAll(buf []byte)
}

type Repository interface {
	// GetAndIncrementMyRumor return false if the last rumor is empty otherwise returns the new rumor to send and create the next rumor
	GetAndIncrementMyRumor() (bool, method2.Rumor, error)

	GetParamsHeartbeat() (map[string][]string, error)
}

type JsonRpcHandler interface {
	Handle(socket socket.Socket, msg []byte) error
}

type GreetServerSender interface {
	SendGreetServer(socket socket.Socket) error
}

type RumorSender interface {
	SendRumor(socket socket.Socket, rumor method2.Rumor)
}

type Hub struct {
	wg               *sync.WaitGroup
	messageChan      chan socket.IncomingMessage
	closedSockets    chan string
	stop             chan struct{}
	resetRumorSender chan struct{}

	// in memory states
	subs    Subscribers
	sockets Sockets

	// database
	db Repository

	// handlers
	jsonRpcHandler    JsonRpcHandler
	greetserverSender GreetServerSender
	rumorSender       RumorSender
}

func New(dbPath string, ownerPubKey kyber.Point, clientAddress, serverAddress string) (*Hub, error) {

	// Initialize the in memory states
	subs := state.NewSubscribers()
	peers := state.NewPeers()
	queries := state.NewQueries(&logger.Logger)
	hubParams := state.NewHubParams()
	sockets := state.NewSockets()

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
	conf := state.CreateConfig(ownerPubKey, serverPublicKey, serverSecretKey, clientAddress, serverAddress)

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
	dataHandlers := hmessage.DataHandlers{
		Root:       hroot.New(conf, &db, subs, peers, schemaValidator),
		Lao:        hlao.New(conf, subs, &db, schemaValidator),
		Election:   helection.New(conf, subs, &db, schemaValidator),
		Chirp:      hchirp.New(conf, subs, &db, schemaValidator),
		Reaction:   hreaction.New(subs, &db, schemaValidator),
		Coin:       hcoin.New(subs, &db, schemaValidator),
		Federation: hfederation.New(hubParams, subs, &db, schemaValidator),
	}

	// Create the message handler
	msgHandler := hmessage.New(&db, dataHandlers)

	// Create the greetserver handler
	greetserverHandler := hgreetserver.New(conf, peers)

	// Create the rumor handler
	rumorHandler := hrumor.New(queries, sockets, &db, msgHandler)

	// Create the query handler
	qHandler := hquery.New(hquery.MethodHandlers{
		Catchup:         hcatchup.New(&db),
		GetMessagesbyid: hgetmessagesbyid.New(&db),
		Greetserver:     greetserverHandler,
		Heartbeat:       hheartbeat.New(queries, &db),
		Publish:         hpublish.New(hubParams, &db, msgHandler),
		Subscribe:       hsubscribe.New(subs),
		Unsubscribe:     hunsubscribe.New(subs),
		Rumor:           rumorHandler,
	})

	// Create the answer handler
	aHandler := hanswer.New(queries, hanswer.Handlers{
		MessageHandler: msgHandler,
		RumorSender:    rumorHandler,
	})

	// Create the json rpc handler
	jsonRpcHandler := hjsonrpc.New(schemaValidator, qHandler, aHandler)

	// Create the hub
	hub := &Hub{
		wg:                hubParams.GetWaitGroup(),
		messageChan:       hubParams.GetMessageChan(),
		closedSockets:     hubParams.GetClosedSockets(),
		stop:              hubParams.GetStopChan(),
		resetRumorSender:  hubParams.GetResetRumorSender(),
		subs:              subs,
		sockets:           sockets,
		db:                &db,
		jsonRpcHandler:    jsonRpcHandler,
		greetserverSender: greetserverHandler,
		rumorSender:       rumorHandler,
	}

	return hub, nil
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	h.sockets.Upsert(socket)
}

func (h *Hub) Start() {
	h.wg.Add(3)
	go h.runHeartbeat()
	go h.runRumorSender()
	go h.runMessageReceiver()
}

func (h *Hub) Stop() {
	close(h.stop)
	h.wg.Wait()
}

func (h *Hub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

func (h *Hub) OnSocketClose() chan<- string {
	return h.closedSockets
}

func (h *Hub) SendGreetServer(socket socket.Socket) error {
	return h.greetserverSender.SendGreetServer(socket)
}

func (h *Hub) runMessageReceiver() {
	defer h.wg.Done()
	defer logger.Logger.Info().Msg("stopping hub")

	logger.Logger.Info().Msg("starting hub")

	for {
		logger.Logger.Debug().Msg("waiting for a new message")
		select {
		case incomingMessage := <-h.messageChan:
			logger.Logger.Debug().Msg("start handling a message")
			err := h.jsonRpcHandler.Handle(incomingMessage.Socket, incomingMessage.Message)
			if err != nil {
				logger.Logger.Error().Err(err)
			} else {
				logger.Logger.Debug().Msg("successfully handled a message")
			}
		case socketID := <-h.closedSockets:
			logger.Logger.Debug().Msgf("stopping the Socket " + socketID)
			h.subs.UnsubscribeFromAll(socketID)
		case <-h.stop:
			return
		}
	}
}

func (h *Hub) runRumorSender() {
	ticker := time.NewTicker(rumorDelay)
	defer ticker.Stop()
	defer h.wg.Done()
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
		case <-h.resetRumorSender:
			logger.Logger.Debug().Msgf("sender rumor reset")
			ticker.Reset(rumorDelay)
			err := h.tryToSendRumor()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.stop:
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
	defer h.wg.Done()
	defer logger.Logger.Info().Msg("stopping heartbeat sender")

	logger.Logger.Info().Msg("starting heartbeat sender")

	for {
		select {
		case <-ticker.C:
			err := h.sendHeartbeat()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.stop:
			return
		}
	}
}

// sendHeartbeat sends a heartbeat message to all servers
func (h *Hub) sendHeartbeat() error {
	heartbeatMessage := mheartbeat.Heartbeat{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
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
