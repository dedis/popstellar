package query

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/state"
)

func handleGreetServer(socket socket.Socket, byteMessage []byte) error {
	var greetServer method.GreetServer
	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	err = state.AddPeerInfo(socket.ID(), greetServer.Params)
	if err != nil {
		return err
	}

	isGreeted, err := state.IsPeerGreeted(socket.ID())
	if err != nil {
		return err
	}
	if isGreeted {
		return nil
	}

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

	return state.AddPeerGreeted(socket.ID())
}
