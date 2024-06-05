package hub

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/logger"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/repository"
	"popstellar/internal/singleton/state"
	"time"
)

const (
	heartbeatDelay = time.Second * 30
	rumorDelay     = time.Second * 5
)

type JsonRpcHandler interface {
	Handle(socket socket.Socket, msg []byte) error
}

type RumorSender interface {
	SendRumor(socket socket.Socket, rumor method.Rumor)
}

type Hub struct {
	conf           repository.ConfigManager
	control        repository.HubManager
	subs           repository.SubscriptionManager
	sockets        repository.SocketManager
	jsonRpcHandler JsonRpcHandler
	rumorSender    RumorSender
	db             repository.HubRepository
}

func New(conf repository.ConfigManager, dbPath string) *Hub {

	return &Hub{}
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
	serverPublicKey, clientAddress, serverAddress, err := h.conf.GetServerInfo()
	if err != nil {
		return err
	}

	greetServerParams := method.GreetServerParams{
		PublicKey:     serverPublicKey,
		ServerAddress: serverAddress,
		ClientAddress: clientAddress,
	}

	serverGreet := &method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGreetServer,
		},
		Params: greetServerParams,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		return errors.NewJsonMarshalError(err.Error())
	}

	socket.Send(buf)

	return state.AddPeerGreeted(socket.ID())
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

	reset, err := state.GetResetRumorSender()
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	for {
		select {
		case <-ticker.C:
			logger.Logger.Debug().Msgf("sender rumor trigerred")
			err = h.tryToSendRumor()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-reset:
			logger.Logger.Debug().Msgf("sender rumor reset")
			ticker.Reset(rumorDelay)
			err = h.tryToSendRumor()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.control.GetStopChan():
			return
		}
	}
}

func (h *Hub) tryToSendRumor() error {
	ok, rumor, err := h.db.GetAndIncrementMyRumor()
	if err != nil {
		return err
	}
	if !ok {
		logger.Logger.Debug().Msg("no new rumor to send")
		return nil
	}

	h.rumorSender.SendRumor(nil, rumor)

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
			err := h.sendHeartbeatToServers()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.control.GetStopChan():
			return
		}
	}
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() error {
	heartbeatMessage := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
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
