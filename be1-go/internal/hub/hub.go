package hub

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler"
	query2 "popstellar/internal/handler/query"
	"popstellar/internal/logger"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/singleton/utils"
	"sync"
	"time"
)

const (
	heartbeatDelay = time.Second * 30
	rumorDelay     = time.Second * 5
)

type Hub struct {
	wg            *sync.WaitGroup
	messageChan   chan socket.IncomingMessage
	stop          chan struct{}
	closedSockets chan string
}

func NewHub() *Hub {
	wg, err := state.GetWaitGroup()
	if err != nil {
		logger.Logger.Err(err)
		return nil
	}

	messageChan, err := state.GetMessageChan()
	if err != nil {
		logger.Logger.Err(err)
		return nil
	}

	stop, err := state.GetStopChan()
	if err != nil {
		logger.Logger.Err(err)
		return nil
	}

	closedSockets, err := state.GetClosedSockets()
	if err != nil {
		logger.Logger.Err(err)
		return nil
	}

	return &Hub{
		wg:            wg,
		messageChan:   messageChan,
		stop:          stop,
		closedSockets: closedSockets,
	}
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	err := state.Upsert(socket)
	if err != nil {
		logger.Logger.Err(err)
	}
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
	serverPublicKey, clientAddress, serverAddress, err := config.GetServerInfo()
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
	defer h.wg.Done()
	defer logger.Logger.Info().Msg("stopping hub")

	logger.Logger.Info().Msg("starting hub")

	for {
		logger.Logger.Debug().Msg("waiting for a new message")
		select {
		case incomingMessage := <-h.messageChan:
			logger.Logger.Debug().Msg("start handling a message")
			err := handler.HandleIncomingMessage(incomingMessage.Socket, incomingMessage.Message)
			if err != nil {
				logger.Logger.Error().Err(err)
			} else {
				logger.Logger.Debug().Msg("successfully handled a message")
			}
		case socketID := <-h.closedSockets:
			logger.Logger.Debug().Msgf("stopping the Socket " + socketID)
			err := state.UnsubscribeFromAll(socketID)
			if err != nil {
				logger.Logger.Error().Err(err)
			}
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
		case <-h.stop:
			return
		}
	}
}

func (h *Hub) tryToSendRumor() error {
	db, err := database.GetRumorSenderRepositoryInstance()
	if err != nil {
		return err
	}

	ok, rumor, err := db.GetAndIncrementMyRumor()
	if err != nil {
		return err
	}
	if !ok {
		logger.Logger.Debug().Msg("no new rumor to send")
		return nil
	}

	query2.SendRumor(nil, rumor)

	return nil
}

func (h *Hub) runHeartbeat() {
	ticker := time.NewTicker(heartbeatDelay)
	defer ticker.Stop()
	defer h.wg.Done()

	for {
		select {
		case <-ticker.C:
			err := h.sendHeartbeatToServers()
			if err != nil {
				logger.Logger.Error().Err(err)
			}
		case <-h.stop:
			utils.LogInfo("stopping the heartbeat")
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

	return state.SendToAllServer(buf)
}
