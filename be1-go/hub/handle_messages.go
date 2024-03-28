package hub

import (
	"golang.org/x/xerrors"
	jsonrpc "popstellar/message"
	"popstellar/validation"
)

func handleMessage(params handlerParameters, msg []byte) error {
	// validate against json schema
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

	var errMsgID *int
	var errMsg error

	switch rpcType {
	case jsonrpc.RPCTypeQuery:
		errQueryID, errQuery := handleQuery(params, msg)
		errMsgID = &errQueryID
		errMsg = errQuery

	case jsonrpc.RPCTypeAnswer:
		errAnswerID, errAnswer := handleAnswer(params, msg)
		errMsgID = &errAnswerID
		errMsg = errAnswer

	default:
		errMsgID = nil
		errMsg = xerrors.New("jsonRPC is of unknown type")
	}

	if err != nil {
		params.socket.SendError(errMsgID, errMsg)
	}

	return errMsg
}

func handleQuery(params handlerParameters, msg []byte) (int, error) {
	return 0, nil
}

func handleAnswer(params handlerParameters, msg []byte) (int, error) {
	return 0, nil
}
