package handler

import (
	"encoding/json"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
)

func handleQuery(socket socket.Socket, msg []byte) *answer.Error {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleQuery")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var id *int = nil
	var errAnswer *answer.Error

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, errAnswer = handleCatchUp(socket, msg)
	case query.MethodGetMessagesById:
		id, errAnswer = handleGetMessagesByID(socket, msg)
	case query.MethodGreetServer:
		id, errAnswer = handleGreetServer(socket, msg)
	case query.MethodHeartbeat:
		errAnswer = handleHeartbeat(socket, msg)
	case query.MethodPublish:
		id, errAnswer = handlePublish(socket, msg)
	case query.MethodSubscribe:
		id, errAnswer = handleSubscribe(socket, msg)
	case query.MethodUnsubscribe:
		id, errAnswer = handleUnsubscribe(socket, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleQuery")
		socket.SendError(id, errAnswer)
		return errAnswer
	}

	return nil
}

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
			JSONRPCBase: jsonrpc.JSONRPCBase{
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

func handleSubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleSubscribe")
	}

	if Root == subscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Subscribe to root channel")
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	errAnswer := state.Subscribe(socket, subscribe.Params.Channel)
	if errAnswer != nil {
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	socket.SendResult(subscribe.ID, nil, nil)

	return &subscribe.ID, nil
}

func handleUnsubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleUnsubscribe")
	}

	if Root == unsubscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Unsubscribe from root channel")
		return &unsubscribe.ID, errAnswer.Wrap("handleUnsubscribe")
	}

	errAnswer := state.Unsubscribe(socket, unsubscribe.Params.Channel)
	if errAnswer != nil {
		return &unsubscribe.ID, errAnswer.Wrap("handleUnsubscribe")
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}

func handlePublish(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handlePublish")
	}

	errAnswer := handleChannel(publish.Params.Channel, publish.Params.Message)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	socket.SendResult(publish.ID, nil, nil)

	return &publish.ID, nil
}

func handleCatchUp(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleCatchUp")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return &catchup.ID, errAnswer.Wrap("handleCatchUp")
	}

	result, err := db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("all message from channel %s: %v", catchup.Params.Channel, err)
		return &catchup.ID, errAnswer.Wrap("handleCatchUp")
	}

	socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}

func handleHeartbeat(socket socket.Socket, byteMessage []byte) *answer.Error {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return errAnswer.Wrap("handleHeartbeat")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleHeartbeat")
	}

	result, err := db.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("params for get messages by id: %v", err)
		return errAnswer.Wrap("handleHeartbeat")
	}

	if len(result) == 0 {
		return nil
	}

	queryId, errAnswer := state.GetNextID()
	if errAnswer != nil {
		return errAnswer.Wrap("handleHeartbeat")
	}

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGetMessagesById,
		},
		ID:     queryId,
		Params: result,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err)
		return errAnswer.Wrap("handleHeartbeat")
	}

	socket.Send(buf)

	errAnswer = state.AddQuery(queryId, getMessagesById)
	if errAnswer != nil {
		return errAnswer.Wrap("handleHeartbeat")
	}

	return nil
}

func handleGetMessagesByID(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleGetMessageByID")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return &getMessagesById.ID, errAnswer.Wrap("handleGetMessageByID")
	}

	result, err := db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("result for get messages by id: %v", err)
		return &getMessagesById.ID, errAnswer.Wrap("handleGetMessageByID")
	}

	socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
