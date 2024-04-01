package hub

import (
	"encoding/base64"
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
)

func handleCatchUp(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleCatchUp")
		return nil, errAnswer
	}

	result, err := params.db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleCatchUp")
		return &catchup.ID, errAnswer
	}

	params.socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}

func handleGetMessagesByID(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal message: %v",
			err).Wrap("handleGetMessageByID")
		return nil, errAnswer
	}

	result, err := params.db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleGetMessageByID")
		return &getMessagesById.ID, errAnswer
	}

	params.socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}

func handleGreetServer(params handlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshal: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	err = params.peers.AddPeerInfo(params.socket.ID(), greetServer.Params)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to add peer: %v", err).Wrap("handleGreetServer")
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
		errAnswer := answer.NewInternalServerError("error while querying db: %v", err).Wrap("sendGreetServer")
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
		errAnswer := answer.NewInternalServerError("failed to marshal: %v",
			err).Wrap("sendGreetServer")
		return errAnswer
	}

	params.socket.Send(buf)

	params.peers.AddPeerGreeted(params.socket.ID())

	return nil
}

func handleHeartbeat(params handlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	result, err := params.db.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	if len(result) == 0 {
		return nil, nil
	}

	errAnswer := sendGetMessagesByID(params, result)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	return nil, nil
}

func sendGetMessagesByID(params handlerParameters, missingIds map[string][]string) *answer.Error {
	queryId := params.queries.GetNextID()

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGetMessagesById,
		},
		ID:     queryId,
		Params: missingIds,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		return answer.NewInternalServerError("failed to marshal: %v", err).Wrap("sendGetMessagesByID")
	}

	params.socket.Send(buf)

	params.queries.AddQuery(queryId, getMessagesById)

	return nil
}

func handlePublish(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handlePublish")
		return nil, errAnswer
	}

	errAnswer := handleChannel(params, publish.Params.Channel, publish.Params.Message)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handlePublish")
		return &publish.ID, errAnswer
	}

	return &publish.ID, nil
}

func handleSubscribe(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleSubscribe")
		return nil, errAnswer
	}

	if rootChannel == subscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot subscribe to root channel").Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	errAnswer := params.subs.subscribe(subscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	return &subscribe.ID, nil
}

func handleUnsubscribe(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleUnsubscribe")
		return nil, errAnswer
	}

	if rootChannel == unsubscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot unsubscribe from root channel").Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	errAnswer := params.subs.unsubscribe(unsubscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	return &unsubscribe.ID, nil
}
