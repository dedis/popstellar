package message

import (
	"encoding/json"
	"popstellar/internal/popserver/types"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/validation"
)

func HandleMessage(params types.HandlerParameters, msg []byte) error {
	err := params.SchemaValidator.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid json: %v", err).Wrap("HandleMessage")
		params.Socket.SendError(nil, errAnswer)
		return errAnswer
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get rpc type: %v", err).Wrap("HandleMessage")
		params.Socket.SendError(nil, errAnswer)
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
		errAnswer = errAnswer.Wrap("HandleMessage")
		params.Socket.SendError(id, errAnswer)
		return errAnswer
	}

	return nil
}

func handleQuery(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleQuery")
		return nil, errAnswer
	}

	var id *int
	var errAnswer *answer.Error

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, errAnswer = handleCatchUp(params, msg)
	case query.MethodGetMessagesById:
		id, errAnswer = handleGetMessagesByID(params, msg)
	case query.MethodGreetServer:
		id, errAnswer = handleGreetServer(params, msg)
	case query.MethodHeartbeat:
		id, errAnswer = handleHeartbeat(params, msg)
	case query.MethodPublish:
		id, errAnswer = handlePublish(params, msg)
	case query.MethodSubscribe:
		id, errAnswer = handleSubscribe(params, msg)
	case query.MethodUnsubscribe:
		id, errAnswer = handleUnsubscribe(params, msg)
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

func handleAnswer(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleAnswer")
		return nil, errAnswer
	}

	if answerMsg.Result == nil {
		params.Log.Warn().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil, nil
	}
	if answerMsg.Result.IsEmpty() {
		params.Log.Info().Msg("result isn't an answer to a query, nothing to handle")
		return nil, nil
	}

	err = params.Queries.SetQueryReceived(*answerMsg.ID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to set query state: %v", err).Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	errAnswer := handleGetMessagesByIDAnswer(params, answerMsg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	return answerMsg.ID, nil
}
