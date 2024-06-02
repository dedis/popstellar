package query

import (
	"encoding/json"
	"popstellar/internal/message"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/state"
)

func handleGreetServer(socket socket.Socket, byteMessage []byte) (*int, *answer.Error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleGreetServer")
	}

	errAnswer := state.AddPeerInfo(socket.ID(), greetServer.Params)
	if errAnswer != nil {
		return nil, errAnswer.Wrap("handleGreetServer")
	}

	isGreeted, errAnswer := state.IsPeerGreeted(socket.ID())
	if errAnswer != nil {
		return nil, errAnswer.Wrap("handleGreetServer")
	}
	if isGreeted {
		return nil, nil
	}

	serverPublicKey, clientAddress, serverAddress, errAnswer := config.GetServerInfo()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("handleGreetServer")
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
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err)
		return nil, errAnswer.Wrap("handleGreetServer")
	}

	socket.Send(buf)

	errAnswer = state.AddPeerGreeted(socket.ID())
	if errAnswer != nil {
		return nil, errAnswer.Wrap("handleGreetServer")
	}

	return nil, nil
}
