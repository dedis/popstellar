package hub

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/validation"
)

func handleMessage(params handlerParameters, msg []byte) error {
	err := params.schemaValidator.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid json: %v", err).Wrap("handleMessage")
		params.socket.SendError(nil, errAnswer)
		return errAnswer
	}

	rpcType, err := jsonrpc.GetType(msg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get rpc type: %v", err).Wrap("handleMessage")
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
		errAnswer = errAnswer.Wrap("handleMessage")
		params.socket.SendError(id, errAnswer)
		return errAnswer
	}

	return nil
}

func handleQuery(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleQuery")
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
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleAnswer")
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
		errAnswer := answer.NewInternalServerError("failed to set query state: %v", err).Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	errAnswer := handleGetMessagesByIdAnswer(params, answerMsg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleAnswer")
		return answerMsg.ID, errAnswer
	}

	return answerMsg.ID, nil
}

func verifyDataAndGetObjectAction(params handlerParameters, msg message.Message) (object string, action string, errAnswer *answer.Error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}

	// validate message data against the json schema
	err = params.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to validate message against json schema: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}

	// get object#action
	object, action, err = messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}
	return object, action, nil
}

func Sign(data []byte, params handlerParameters) ([]byte, error) {

	serverSecretBuf, err := params.db.GetServerSecretKey()
	if err != nil {
		return nil, xerrors.Errorf("failed to get the server secret key")
	}

	serverSecretKey := crypto.Suite.Scalar()
	err = serverSecretKey.UnmarshalBinary(serverSecretBuf)
	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}

func broadcastToAllClients(msg message.Message, params handlerParameters, channel string) *answer.Error {
	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			channel,
			msg,
		},
	}
	var errAnswer *answer.Error
	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal broadcast query: %v", err)
		errAnswer = errAnswer.Wrap("broadcastToAllClients")
		return errAnswer
	}

	errAnswer = params.subs.SendToAll(buf, channel)
	if err != nil {
		errAnswer = errAnswer.Wrap("broadcastToAllClients")
		return errAnswer
	}

	return nil
}
