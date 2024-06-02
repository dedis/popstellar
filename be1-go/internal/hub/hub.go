package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/internal/handler/low"
	"popstellar/internal/logger"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	state2 "popstellar/internal/singleton/state"
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
	wg, errAnswer := state2.GetWaitGroup()
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
		return nil
	}

	messageChan, errAnswer := state2.GetMessageChan()
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
		return nil
	}

	stop, errAnswer := state2.GetStopChan()
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
		return nil
	}

	closedSockets, errAnswer := state2.GetClosedSockets()
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
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
	errAnswer := state2.Upsert(socket)
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
	}
}

func (h *Hub) Start() {
	h.wg.Add(3)
	go func() {
		ticker := time.NewTicker(heartbeatDelay)
		defer ticker.Stop()
		defer h.wg.Done()

		for {
			select {
			case <-ticker.C:
				h.sendHeartbeatToServers()
			case <-h.stop:
				utils.LogInfo("stopping the heartbeat")
				return
			}
		}
	}()
	go func() {
		ticker := time.NewTicker(rumorDelay)
		defer ticker.Stop()
		defer h.wg.Done()
		defer logger.Logger.Info().Msg("stopping rumor sender")

		logger.Logger.Info().Msg("starting rumor sender")

		reset, errAnswer := state2.GetResetRumorSender()
		if errAnswer != nil {
			logger.Logger.Error().Err(errAnswer)
			return
		}

		for {
			select {
			case <-ticker.C:
				logger.Logger.Debug().Msgf("sender rumor trigerred")
				h.tryToSendRumor()
			case <-reset:
				logger.Logger.Debug().Msgf("sender rumor reset")
				ticker.Reset(rumorDelay)
				h.tryToSendRumor()
			case <-h.stop:
				return
			}
		}
	}()
	go func() {
		defer h.wg.Done()

		utils.LogInfo("start the Hub")
		for {
			utils.LogInfo("waiting for a new message")
			select {
			case incomingMessage := <-h.messageChan:
				utils.LogInfo("start handling a message")
				err := low.HandleIncomingMessage(incomingMessage.Socket, incomingMessage.Message)
				if err != nil {
					utils.LogError(err)
				} else {
					utils.LogInfo("successfully handled a message")
				}
			case socketID := <-h.closedSockets:
				utils.LogInfo("stopping the Socket " + socketID)
				state2.UnsubscribeFromAll(socketID)
			case <-h.stop:
				utils.LogInfo("stopping the Hub")
				return
			}
		}
	}()
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
	serverPublicKey, clientAddress, serverAddress, errAnswer := config.GetServerInfo()
	if errAnswer != nil {
		return xerrors.Errorf(errAnswer.Error())
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
		return xerrors.Errorf("failed to marshal: %v", err)
	}

	socket.Send(buf)

	errAnswer = state2.AddPeerGreeted(socket.ID())
	if errAnswer != nil {
		return xerrors.Errorf(errAnswer.Error())
	}
	return nil
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() {
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
		logger.Logger.Err(err)
	}

	errAnswer := state2.SendToAllServer(buf)
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
	}
}

func (h *Hub) tryToSendRumor() {
	db, errAnswer := database.GetRumorSenderRepositoryInstance()
	if errAnswer != nil {
		logger.Logger.Error().Err(errAnswer)
		return
	}

	ok, rumor, err := db.GetAndIncrementMyRumor()
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}
	if !ok {
		logger.Logger.Debug().Msg("no new rumor to send")
		return
	}

	low.SendRumor(nil, rumor)
}
