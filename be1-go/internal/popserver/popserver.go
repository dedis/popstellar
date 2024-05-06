package popserver

import (
	"encoding/base64"
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/message"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/utils"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
)

type PopServer struct {
	messageChan   chan socket.IncomingMessage
	stop          chan struct{}
	closedSockets chan string
}

func NewPopServer() *PopServer {
	return &PopServer{
		messageChan:   make(chan socket.IncomingMessage),
		stop:          make(chan struct{}),
		closedSockets: make(chan string),
	}
}

func (p *PopServer) NotifyNewServer(socket socket.Socket) {
	return
}

func (p *PopServer) Start() {
	go func() {
		log, ok := utils.GetLogInstance()
		if !ok {
			panic("Missing log")
		}

		log.Info().Msg("Start check messages")
		for {
			select {
			case incomingMessage := <-p.messageChan:
				err := message.HandleMessage(incomingMessage.Socket, incomingMessage.Message)
				if err != nil {
					log.Error().Msg(err.Error())
				}
			case <-p.closedSockets:
				log.Info().Msg("stopping the sockets")
				return
			case <-p.stop:
				log.Info().Msg("stopping the hub")
				return
			}
		}
	}()
}

func (p *PopServer) Stop() {
	close(p.stop)
}

func (p *PopServer) Receiver() chan<- socket.IncomingMessage {
	return p.messageChan
}

func (p *PopServer) OnSocketClose() chan<- string {
	return p.closedSockets
}

func (p *PopServer) SendGreetServer(socket socket.Socket) error {
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

	peers, ok := state.GetPeersInstance()
	if !ok {
		return xerrors.Errorf("failed to get state")
	}

	peers.AddPeerGreeted(socket.ID())

	return nil
}
