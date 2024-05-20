package popserver

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/handler"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/internal/popserver/utils"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"sync"
	"time"
)

const (
	heartbeatDelay           = time.Second * 1000
	rumorDelay               = time.Second * 30
	thresholdMessagesByRumor = 3
)

type Hub struct {
	messageChan   chan socket.IncomingMessage
	stop          chan struct{}
	closedSockets chan string
	serverSockets types.Sockets
	wg            sync.WaitGroup
}

func NewHub() *Hub {
	return &Hub{
		messageChan:   make(chan socket.IncomingMessage),
		stop:          make(chan struct{}),
		closedSockets: make(chan string),
		serverSockets: types.NewSockets(),
	}
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	h.serverSockets.Upsert(socket)
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
		defer popstellar.Logger.Info().Msg("stopping rumor sender")

		popstellar.Logger.Debug().Msg("starting rumor sender")

		cSendRumor, errAnswer := state.GetChanSendRumor()
		if errAnswer != nil {
			popstellar.Logger.Error().Err(errAnswer)
			return
		}

		cSendAgainRumor, errAnswer := state.GetChanSendAgainRumor()
		if errAnswer != nil {
			popstellar.Logger.Error().Err(errAnswer)
			return
		}

		db, errAnswer := database.GetRumorSenderRepositoryInstance()
		if errAnswer != nil {
			popstellar.Logger.Error().Err(errAnswer)
			return
		}

		for {
			select {
			case <-ticker.C:
				popstellar.Logger.Debug().Msgf("sender rumor trigerred")
				h.tryToSendRumor()
			case msgID := <-cSendRumor:
				popstellar.Logger.Debug().Msgf("sender rumor need to add message %s", msgID)
				nbMessagesInsideRumor, err := db.AddMessageToMyRumor(msgID)
				if err != nil {
					popstellar.Logger.Error().Err(err)
					break
				}

				if nbMessagesInsideRumor < thresholdMessagesByRumor {
					popstellar.Logger.Debug().Msgf("sender rumor need to add message %s", msgID)
					break
				}

				ticker.Reset(rumorDelay)
				h.tryToSendRumor()
			case queryID := <-cSendAgainRumor:
				popstellar.Logger.Debug().Msgf("sender rumor need to continue sending query %d", queryID)
				rumor, ok, errAnswer := state.GetRumorFromPastQuery(queryID)
				if errAnswer != nil {
					popstellar.Logger.Error().Err(errAnswer)
					break
				}
				if !ok {
					popstellar.Logger.Debug().Msgf("rumor query %d doesn't exist", queryID)
					break
				}

				h.sendRumor(rumor)
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
				err := handler.HandleIncomingMessage(incomingMessage.Socket, incomingMessage.Message)
				if err != nil {
					utils.LogError(err)
				} else {
					utils.LogInfo("successfully handled a message")
				}
			case socketID := <-h.closedSockets:
				utils.LogInfo("stopping the Socket " + socketID)
				state.UnsubscribeFromAll(socketID)
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

	errAnswer = state.AddPeerGreeted(socket.ID())
	if errAnswer != nil {
		return xerrors.Errorf(errAnswer.Error())
	}
	return nil
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() {

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return
	}

	params, err := db.GetParamsHeartbeat()
	if err != nil {
		return
	}

	heartbeatMessage := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "heartbeat",
		},
		Params: params,
	}

	buf, err := json.Marshal(heartbeatMessage)
	if err != nil {
		utils.LogError(err)
	}
	h.serverSockets.SendToAll(buf)
}

func (h *Hub) tryToSendRumor() {
	db, errAnswer := database.GetRumorSenderRepositoryInstance()
	if errAnswer != nil {
		popstellar.Logger.Error().Err(errAnswer)
		return
	}

	ok, rumor, err := db.GetAndIncrementMyRumor()
	if err != nil {
		popstellar.Logger.Error().Err(err)
		return
	}
	if !ok {
		popstellar.Logger.Info().Msg("no new ")
		return
	}

	h.sendRumor(rumor)
}

func (h *Hub) sendRumor(rumor method.Rumor) {
	id, errAnswer := state.GetNextID()
	if errAnswer != nil {
		popstellar.Logger.Error().Err(errAnswer)
		return
	}

	rumor.ID = id

	errAnswer = state.AddRumorQuery(id, rumor)
	if errAnswer != nil {
		popstellar.Logger.Error().Err(errAnswer)
		return
	}

	buf, err := json.Marshal(rumor)
	if err != nil {
		popstellar.Logger.Error().Err(err)
		return
	}

	popstellar.Logger.Debug().Msgf("sending rumor %s-%d query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)
	h.serverSockets.SendRumor(buf)
}
