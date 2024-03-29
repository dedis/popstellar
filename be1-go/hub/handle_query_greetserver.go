package hub

import (
	"encoding/base64"
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
)

func handleGreetServer(params handlerParameters, byteMessage []byte) (*int, error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		return nil, answer.NewInternalServerError("failed to unmarshal greetServer message: %v", err)
	}

	err = params.peers.AddPeerInfo(params.socket.ID(), greetServer.Params)
	if err != nil {
		return nil, err
	}

	if params.peers.IsPeerGreeted(params.socket.ID()) {
		return nil, nil
	}

	errA := sendGreetServer(params)
	if errA != nil {
		return nil, answer.NewErrorf(errA.Code, "failed to send greetServer message: %v", err)
	}

	return nil, nil
}

func sendGreetServer(params handlerParameters) *answer.Error {
	pkBytes, err := params.db.GetServerPubKey()
	if err != nil {
		return answer.NewInternalServerError("was not able to fetch the pk of server")
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
		return answer.NewInternalServerError("failed to marshal server greet: %v", err)
	}

	params.socket.Send(buf)

	params.peers.AddPeerGreeted(params.socket.ID())

	return nil
}
