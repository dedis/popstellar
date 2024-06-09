package greetserver

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
)

type Config interface {
	GetServerInfo() (string, string, string, error)
}

type Peers interface {
	AddPeerInfo(socketID string, info method.GreetServerParams) error
	IsPeerGreeted(socketID string) bool
	AddPeerGreeted(socketID string)
}

type Handler struct {
	conf  Config
	peers Peers
}

func New(conf Config, peers Peers) *Handler {
	return &Handler{
		conf:  conf,
		peers: peers,
	}
}

func (h *Handler) Handle(socket socket.Socket, byteMessage []byte) (*int, error) {
	var greetServer method.GreetServer
	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	err = h.peers.AddPeerInfo(socket.ID(), greetServer.Params)
	if err != nil {
		return nil, err
	}

	isGreeted := h.peers.IsPeerGreeted(socket.ID())
	if isGreeted {
		return nil, nil
	}

	err = h.SendGreetServer(socket)
	if err != nil {
		return nil, err
	}

	return nil, nil
}

func (h *Handler) SendGreetServer(socket socket.Socket) error {
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
			JSONRPCBase: message.JSONRPCBase{
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

	h.peers.AddPeerGreeted(socket.ID())

	return nil
}
