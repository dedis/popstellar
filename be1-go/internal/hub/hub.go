package hub

import (
	"encoding/json"
	"errors"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/database/sqlite"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/answer/hanswer"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/chirp/hchirp"
	"popstellar/internal/handler/channel/coin/hcoin"
	"popstellar/internal/handler/channel/election/helection"
	"popstellar/internal/handler/channel/federation/hfederation"
	"popstellar/internal/handler/channel/lao/hlao"
	"popstellar/internal/handler/channel/reaction/hreaction"
	"popstellar/internal/handler/channel/root/hroot"
	"popstellar/internal/handler/jsonrpc/hjsonrpc"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/hmessage"
	"popstellar/internal/handler/method/catchup/hcatchup"
	"popstellar/internal/handler/method/getmessagesbyid/hgetmessagesbyid"
	"popstellar/internal/handler/method/greetserver/hgreetserver"
	"popstellar/internal/handler/method/heartbeat/hheartbeat"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	"popstellar/internal/handler/method/publish/hpublish"
	"popstellar/internal/handler/method/rumor/hrumor"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumorstate/hrumorstate"
	"popstellar/internal/handler/method/subscribe/hsubscribe"
	"popstellar/internal/handler/method/unsubscribe/hunsubscribe"
	"popstellar/internal/handler/query/hquery"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"sync"
	"time"
)

const (
	heartbeatDelay  = time.Second * 30
	rumorDelay      = time.Second * 5
	rumorStateDelay = time.Second * 10
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
	GetAndIncrementMyRumor() (bool, mrumor.ParamsRumor, error)

	GetParamsHeartbeat() (map[string][]string, error)

	GetRumorTimestamp() (mrumor.RumorTimestamp, error)
}

type JsonRpcHandler interface {
	Handle(socket socket.Socket, msg []byte) error
}

type GreetServerSender interface {
	SendGreetServer(socket socket.Socket) error
}

type RumorSender interface {
	SendRumor(socket socket.Socket, rumor mrumor.ParamsRumor)
}

type RumorStateSender interface {
	SendRumorState() error
	SendRumorStateTo(socket socket.Socket) error
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
	rumorStateSender  RumorStateSender

	log zerolog.Logger
}

func New(dbPath string, ownerPubKey kyber.Point, clientAddress, serverAddress string, log zerolog.Logger) (*Hub, error) {

	// Initialize the in memory states
	subs := state.NewSubscribers(log)
	peers := state.NewPeers(log)
	queries := state.NewQueries(log)
	hubParams := state.NewHubParams(log)
	sockets := state.NewSockets(log)

	// Initialize the database
	db, err := sqlite.NewSQLite(dbPath, true, log)
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
	conf := state.CreateConfig(ownerPubKey, serverPublicKey, serverSecretKey, clientAddress, serverAddress, log)

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

	// Create the message channel handlers
	channelHandlers := make(hmessage.ChannelHandlers)
	channelHandlers[channel.RootObject] = hroot.New(conf, &db, subs, peers, schemaValidator, log)
	channelHandlers[channel.LAOObject] = hlao.New(conf, subs, &db, schemaValidator, log)
	channelHandlers[channel.ElectionObject] = helection.New(conf, subs, &db, schemaValidator, log)
	channelHandlers[channel.ChirpObject] = hchirp.New(conf, subs, &db, schemaValidator, log)
	channelHandlers[channel.ReactionObject] = hreaction.New(subs, &db, schemaValidator, log)
	channelHandlers[channel.CoinObject] = hcoin.New(subs, &db, schemaValidator, log)

	// Create the message handler
	msgHandler := hmessage.New(&db, channelHandlers, log)

	// Create the greetserver handler
	greetserverHandler := hgreetserver.New(conf, peers, log)

	// Create the rumor handler
	rumorHandler := hrumor.New(queries, sockets, &db, msgHandler, log)

	// Create the rumor state handler
	rumorStateHandler := hrumorstate.New(queries, sockets, &db, log)

	// Create the federation handler
	federationHandler := hfederation.New(hubParams, subs, sockets, conf, &db,
		rumorStateHandler, schemaValidator, log)

	// Create the query handler
	methodHandlers := make(hquery.MethodHandlers)
	methodHandlers[mquery.MethodCatchUp] = hcatchup.New(&db, log)
	methodHandlers[mquery.MethodGetMessagesById] = hgetmessagesbyid.New(&db, log)
	methodHandlers[mquery.MethodGreetServer] = greetserverHandler
	methodHandlers[mquery.MethodHeartbeat] = hheartbeat.New(queries, &db, log)
	methodHandlers[mquery.MethodPublish] = hpublish.New(hubParams, &db, msgHandler, federationHandler, log)
	methodHandlers[mquery.MethodSubscribe] = hsubscribe.New(subs, log)
	methodHandlers[mquery.MethodUnsubscribe] = hunsubscribe.New(subs, log)
	methodHandlers[mquery.MethodRumor] = rumorHandler
	methodHandlers[mquery.MethodRumorState] = rumorStateHandler

	qHandler := hquery.New(methodHandlers, log)

	// Create the answer handler
	aHandler := hanswer.New(queries, hanswer.Handlers{
		MessageHandler: msgHandler,
		RumorHandler:   rumorHandler,
	}, log)

	// Create the json rpc handler
	jsonRpcHandler := hjsonrpc.New(schemaValidator, qHandler, aHandler, log)

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
		rumorStateSender:  rumorStateHandler,
		log:               log.With().Str("module", "hub").Logger(),
	}

	return hub, nil
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	h.sockets.Upsert(socket)
	err := h.rumorStateSender.SendRumorStateTo(socket)
	if err != nil {
		h.log.Error().Err(err).Send()
	}
}

func (h *Hub) Start() {
	h.wg.Add(4)
	go h.runHeartbeat()
	go h.runRumorSender()
	go h.runMessageReceiver()
	go h.runRumorState()
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
	defer h.log.Info().Msg("stopping hub")

	h.log.Info().Msg("starting hub")

	for {
		h.log.Debug().Msg("waiting for a new message")
		select {
		case incomingMessage := <-h.messageChan:
			h.log.Debug().Msg("start handling a message")
			err := h.jsonRpcHandler.Handle(incomingMessage.Socket, incomingMessage.Message)
			if err != nil {
				popError := &poperrors.PopError{}
				if !errors.As(err, &popError) {
					popError = poperrors.NewPopError(poperrors.InternalServerErrorCode, err.Error())
				}

				stack, err := popError.GetStackTraceJSON()
				if err != nil {
					h.log.Error().Err(err).Msg("failed to get stacktrace")
					h.log.Error().Err(popError).Msg("")
				} else {
					h.log.Error().Err(popError).RawJSON("stacktrace", stack).Msg("")
				}

			} else {
				h.log.Debug().Msg("successfully handled a message")
			}
		case socketID := <-h.closedSockets:
			h.log.Debug().Msgf("stopping the Socket " + socketID)
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
	defer h.log.Info().Msg("stopping rumor sender")

	h.log.Info().Msg("starting rumor sender")

	for {
		select {
		case <-ticker.C:
			h.log.Debug().Msgf("sender rumor trigerred")
			err := h.tryToSendRumor()
			if err != nil {
				h.log.Error().Err(err).Msg("")
			}
		case <-h.resetRumorSender:
			h.log.Debug().Msgf("sender rumor reset")
			ticker.Reset(rumorDelay)
			err := h.tryToSendRumor()
			if err != nil {
				h.log.Error().Err(err).Msg("")
			}
		case <-h.stop:
			return
		}
	}
}

func (h *Hub) tryToSendRumor() error {
	ok, params, err := h.db.GetAndIncrementMyRumor()
	if err != nil {
		h.log.Err(err).Msg("")
		return err
	}
	if !ok {
		h.log.Debug().Msg("no new rumor to send")
		return nil
	}

	h.rumorSender.SendRumor(nil, params)

	return nil
}

func (h *Hub) runHeartbeat() {
	ticker := time.NewTicker(heartbeatDelay)
	defer ticker.Stop()
	defer h.wg.Done()
	defer h.log.Info().Msg("stopping heartbeat sender")

	h.log.Info().Msg("starting heartbeat sender")

	for {
		select {
		case <-ticker.C:
			err := h.sendHeartbeat()
			if err != nil {
				h.log.Error().Err(err).Msg("")
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
		return poperrors.NewJsonMarshalError(err.Error())
	}

	h.sockets.SendToAll(buf)

	return nil
}

func (h *Hub) runRumorState() {
	ticker := time.NewTicker(rumorStateDelay)
	defer ticker.Stop()
	defer h.wg.Done()
	defer h.log.Info().Msg("stopping rumor state sender")

	h.log.Info().Msg("starting rumor state sender")

	for {
		select {
		case <-ticker.C:
			err := h.rumorStateSender.SendRumorState()
			if err != nil {
				h.log.Error().Err(err).Msg("")
			}
		case <-h.stop:
			return
		}
	}
}
