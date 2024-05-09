package message

import (
	"encoding/json"
	"fmt"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/util"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/network/socket"
	"popstellar/validation"
)

func HandleMessage(socket socket.Socket, msg []byte) error {
	err := util.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid json: %v", err).Wrap("HandleMessage")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get rpc type: %v", err).Wrap("HandleMessage")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var errAnswer *answer.Error

	switch rpcType {
	case jsonrpc.RPCTypeQuery:
		errAnswer = handleQuery(socket, msg)
	case jsonrpc.RPCTypeAnswer:
		errAnswer = handleAnswer(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("HandleMessage")
		return errAnswer
	}

	return nil
}

func handleQuery(socket socket.Socket, msg []byte) *answer.Error {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleQuery")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var id *int
	var errAnswer *answer.Error

	fmt.Println("Received query of type " + queryBase.Method)

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, errAnswer = handleCatchUp(socket, msg)
	case query.MethodGetMessagesById:
		id, errAnswer = handleGetMessagesByID(socket, msg)
	case query.MethodGreetServer:
		id, errAnswer = handleGreetServer(socket, msg)
	case query.MethodHeartbeat:
		id, errAnswer = handleHeartbeat(socket, msg)
	case query.MethodPublish:
		id, errAnswer = handlePublish(socket, msg)
	case query.MethodSubscribe:
		id, errAnswer = handleSubscribe(socket, msg)
	case query.MethodUnsubscribe:
		id, errAnswer = handleUnsubscribe(socket, msg)
	default:
		id = nil
		errAnswer = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleQuery")
		socket.SendError(id, errAnswer)
		return errAnswer
	}

	return nil
}

func handleAnswer(msg []byte) *answer.Error {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleAnswer")
		return errAnswer
	}

	if answerMsg.Result == nil {
		util.LogInfo("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}
	if answerMsg.Result.IsEmpty() {
		util.LogInfo("expected isn't an answer to a query, nothing to handle")
		return nil
	}

	queries, ok := state.GetQueriesInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleAnswer")
		return errAnswer
	}

	err = queries.SetQueryReceived(*answerMsg.ID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to set query state: %v", err).Wrap("handleAnswer")
		return errAnswer
	}

	errAnswer := handleGetMessagesByIDAnswer(answerMsg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleAnswer")
		return errAnswer
	}

	return nil
}
