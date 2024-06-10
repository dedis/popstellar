package hgreetserver

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
)

type Config interface {
	GetServerInfo() (string, string, string, error)
}

type Peers interface {
	AddPeerInfo(socketID string, info mgreetserver.GreetServerParams) error
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
	var greetServer mgreetserver.GreetServer
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

	greetServerParams := mgreetserver.GreetServerParams{
		PublicKey:     serverPublicKey,
		ServerAddress: serverAddress,
		ClientAddress: clientAddress,
	}

	serverGreet := &mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
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
