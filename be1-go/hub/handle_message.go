package hub

import (
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/validation"
)

func handleMessage(params handlerParameters, msg []byte) error {
	err := params.schemaValidator.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		schemaErr := answer.NewInvalidMessageFieldError("message is not valid against json schema: %v",
			err).Wrap("handleMessage")
		params.socket.SendError(nil, schemaErr)
		return schemaErr
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		rpcErr := answer.NewInvalidMessageFieldError("failed to get rpc type: %v",
			err).Wrap("handleMessage")
		params.socket.SendError(nil, rpcErr)
		return rpcErr
	}

	var errID *int
	var errA *answer.Error

	switch rpcType {
	case jsonrpc.RPCTypeQuery:
		errID, errA = handleQuery(params, msg)

	case jsonrpc.RPCTypeAnswer:
		errID, errA = handleAnswer(params, msg)

	default:
		errID = nil
		errA = answer.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	if errA != nil {
		params.socket.SendError(errID, errA.Wrap("handleMessage"))
	}

	return errA
}

func handleQuery(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errA := answer.NewInvalidMessageFieldError("failed to unmarshal incoming message: %v",
			err).Wrap("handleQuery")
		return nil, errA
	}

	var errID *int
	var errA *answer.Error

	switch queryBase.Method {
	case query.MethodGreetServer:
		errID, errA = handleGreetServer(params, msg)
	case query.MethodHeartbeat:
		errID, errA = handleHeartbeat(params, msg)
	case query.MethodGetMessagesById:
		errID, errA = handleGetMessagesByID(params, msg)
	case query.MethodPublish:
		errID, errA = handlePublish(params, msg)
	case query.MethodSubscribe:
		errID, errA = handleSubscribe(params, msg)
	case query.MethodUnsubscribe:
		errID, errA = handleUnsubscribe(params, msg)
	case query.MethodCatchUp:
		errID, errA = handleCatchUp(params, msg)
	default:
		errID = nil
		errA = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
	}

	if errA != nil {
		return errID, errA.Wrap("handleQuery")
	}

	return errID, nil
}

func handleAnswer(params handlerParameters, msg []byte) (*int, *answer.Error) {

	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal answer: %v",
			err).Wrap("handleAnswer")
	}

	if answerMsg.Result == nil {
		params.log.Warn().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil, nil
	}
	if answerMsg.Result.IsEmpty() {
		params.log.Info().Msg("result isn't an answer to a query, nothing to handle")
		return nil, nil
	}

	err = params.queries.SetQueryReceived(*answerMsg.ID)
	if err != nil {
		return answerMsg.ID, answer.NewInternalServerError("failed to set query state: %v",
			err).Wrap("handleAnswer")
	}

	errA := handleGetMessagesByIdAnswer(params, answerMsg)
	if errA != nil {
		return answerMsg.ID, errA.Wrap("handleAnswer")
	}

	return answerMsg.ID, nil
}
