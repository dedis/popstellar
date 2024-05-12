package popserver

import (
	"encoding/json"
	"golang.org/x/xerrors"
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
	"time"
)

const heartbeatDelay = 30 * time.Second

type Hub struct {
	messageChan   chan socket.IncomingMessage
	stop          chan struct{}
	closedSockets chan string
	serverSockets types.Sockets
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
	go func() {
		ticker := time.NewTicker(heartbeatDelay)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				h.sendHeartbeatToServers()
			case <-h.stop:
				utils.LogInfo("stopping the hub")
				return
			}
		}
	}()
	go func() {
		utils.LogInfo("start the Hub")
		for {
			select {
			case incomingMessage := <-h.messageChan:
				utils.LogInfo("start handling a message")
				err := handler.HandleIncomingMessage(incomingMessage.Socket, incomingMessage.Message)
				if err != nil {
					utils.LogError(err)
				} else {
					utils.LogInfo("successfully handled a message")
				}
			case <-h.closedSockets:
				utils.LogInfo("stopping the Sockets")
				return
			case <-h.stop:
				utils.LogInfo("stopping the Hub")
				return
			}
		}
	}()
}

func (h *Hub) Stop() {
	close(h.stop)
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
