package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/validation"
)

func handleMessage(params handlerParameters, msg []byte) error {
	err := params.schemaValidator.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		schemaErr := xerrors.Errorf("message is not valid against json schema: %v", err)
		params.socket.SendError(nil, schemaErr)
		return schemaErr
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		rpcErr := xerrors.Errorf("failed to get rpc type: %v", err)
		params.socket.SendError(nil, rpcErr)
		return rpcErr
	}

	var errID *int

	switch rpcType {
	case jsonrpc.RPCTypeQuery:
		errID, err = handleQuery(params, msg)

	case jsonrpc.RPCTypeAnswer:
		errID, err = handleAnswer(params, msg)

	default:
		errID = nil
		err = xerrors.New("jsonRPC is of unknown type")
	}

	if err != nil {
		params.socket.SendError(errID, err)
	}

	return err
}

func handleQuery(params handlerParameters, msg []byte) (*int, error) {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		return nil, err
	}

	var errID *int

	switch queryBase.Method {
	case query.MethodGreetServer:
		errID, err = handleGreetServer(params, msg)
	case query.MethodHeartbeat:
		errID, err = handleHeartbeat(params, msg)
	case query.MethodGetMessagesById:
		errID, err = handleGetMessagesById(params, msg)
	case query.MethodPublish:
		errID, err = handlePublish(params, msg)
	case query.MethodSubscribe:
		errID, err = handleSubscribe(params, msg)
	case query.MethodUnsubscribe:
		errID, err = handleUnsubscribe(params, msg)
	case query.MethodCatchUp:
		errID, err = handleCatchUp(params, msg)
	default:
		errID = nil
		err = answer.NewErrorf(-2, "unexpected method: '%s'", queryBase.Method)
	}

	return errID, err
}

func handleAnswer(params handlerParameters, msg []byte) (*int, error) {

	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal answer: %v", err)
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
		return answerMsg.ID, xerrors.Errorf("failed to set query state: %v", err)
	}

	return handleGetMessagesByIdAnswer(params, answerMsg)
}
