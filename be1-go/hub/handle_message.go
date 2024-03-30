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
		errAnswer := answer.NewInvalidMessageFieldError("message is not valid against json schema: %v",
			err).Wrap("handleMessage")
		params.socket.SendError(nil, errAnswer)
		return errAnswer
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get rpc type: %v",
			err).Wrap("handleMessage")
		params.socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var id *int
	var errAnswer *answer.Error

	switch rpcType {
	case jsonrpc.RPCTypeQuery:
		id, errAnswer = handleQuery(params, msg)
	case jsonrpc.RPCTypeAnswer:
		id, errAnswer = handleAnswer(params, msg)
	default:
		id = nil
		errAnswer = answer.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	if errAnswer != nil {
		params.socket.SendError(id, errAnswer.Wrap("handleMessage"))
	}

	return errAnswer
}

func handleQuery(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal incoming message: %v",
			err).Wrap("handleQuery")
		return nil, errAnswer
	}

	var id *int
	var errAnswer *answer.Error

	switch queryBase.Method {
	case query.MethodGreetServer:
		id, errAnswer = handleGreetServer(params, msg)
	case query.MethodHeartbeat:
		id, errAnswer = handleHeartbeat(params, msg)
	case query.MethodGetMessagesById:
		id, errAnswer = handleGetMessagesByID(params, msg)
	case query.MethodPublish:
		id, errAnswer = handlePublish(params, msg)
	case query.MethodSubscribe:
		id, errAnswer = handleSubscribe(params, msg)
	case query.MethodUnsubscribe:
		id, errAnswer = handleUnsubscribe(params, msg)
	case query.MethodCatchUp:
		id, errAnswer = handleCatchUp(params, msg)
	default:
		id = nil
		errAnswer = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleQuery")
		return id, errAnswer
	}

	return id, nil
}

func handleAnswer(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal answer: %v",
			err).Wrap("handleAnswer")
		return nil, errAnswer
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
		errAnswer := answer.NewInternalServerError("failed to set query state: %v",
			err).Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	errAnswer := handleGetMessagesByIdAnswer(params, answerMsg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	return answerMsg.ID, nil
}
