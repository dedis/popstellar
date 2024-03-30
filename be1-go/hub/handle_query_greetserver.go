package hub

import (
	"encoding/base64"
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
)

func handleGreetServer(params handlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshal greetServer message: %v",
			err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	err = params.peers.AddPeerInfo(params.socket.ID(), greetServer.Params)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to add peer : %v",
			err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	if params.peers.IsPeerGreeted(params.socket.ID()) {
		return nil, nil
	}

	errAnswer := sendGreetServer(params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleGreetServer")
		return nil, errAnswer
	}

	return nil, nil
}

func sendGreetServer(params handlerParameters) *answer.Error {
	pkBytes, err := params.db.GetServerPubKey()
	if err != nil {
		errAnswer := answer.NewInternalServerError("was not able to fetch the pk of server: %v",
			err).Wrap("sendGreetServer")
		return errAnswer
	}

	serverInfo := method.ServerInfo{
		PublicKey:     base64.URLEncoding.EncodeToString(pkBytes),
		ServerAddress: params.serverServerAddress,
		ClientAddress: params.clientServerAddress,
	}

	serverGreet := &method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGreetServer,
		},
		Params: serverInfo,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal server greet: %v",
			err).Wrap("sendGreetServer")
		return errAnswer
	}

	params.socket.Send(buf)

	params.peers.AddPeerGreeted(params.socket.ID())

	return nil
}
