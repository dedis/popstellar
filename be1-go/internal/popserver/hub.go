package popserver

import (
	"encoding/base64"
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/handler"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/utils"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
)

type Hub struct {
	messageChan   chan socket.IncomingMessage
	stop          chan struct{}
	closedSockets chan string
}

func NewHub() *Hub {
	return &Hub{
		messageChan:   make(chan socket.IncomingMessage),
		stop:          make(chan struct{}),
		closedSockets: make(chan string),
	}
}

func (h *Hub) NotifyNewServer(socket socket.Socket) {
	return
}

func (h *Hub) Start() {
	go func() {
		utils.LogInfo("Start check messages")
		for {
			select {
			case incomingMessage := <-h.messageChan:
				err := handler.HandleIncomingMessage(incomingMessage.Socket, incomingMessage.Message)
				if err != nil {
					utils.LogError(err)
				}
			case <-h.closedSockets:
				utils.LogInfo("Start check messages")
				return
			case <-h.stop:
				utils.LogInfo("Start check messages")
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
	pk, clientAddress, serverAddress, ok := config.GetServerInfo()
	if !ok {
		return xerrors.Errorf("failed to get config")
	}

	pkBuf, err := pk.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to unmarshall server public key: %v", err)
	}

	greetServerParams := method.GreetServerParams{
		PublicKey:     base64.URLEncoding.EncodeToString(pkBuf),
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

	return state.AddPeerGreeted(socket.ID())
}
