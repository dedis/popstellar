package handler

import (
	"popstellar/internal/popserver/utils"
	"popstellar/message"
	"popstellar/message/answer"
	"popstellar/network/socket"
	"popstellar/validation"
)

func HandleIncomingMessage(socket socket.Socket, msg []byte) error {
	errAnswer := utils.VerifyJSON(msg, validation.GenericMessage)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleMessage")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	rpcType, err := message.GetType(msg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get rpc type: %v", err).Wrap("handleMessage")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	switch rpcType {
	case message.RPCTypeQuery:
		errAnswer = handleQuery(socket, msg)
	case message.RPCTypeAnswer:
		errAnswer = handleAnswer(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleMessage")
		return errAnswer
	}

	return nil
}
