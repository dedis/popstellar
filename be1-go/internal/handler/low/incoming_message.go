package low

import (
	jsonrpc "popstellar/internal/handler/low/answer"
	"popstellar/internal/handler/low/query"
	"popstellar/internal/message"
	"popstellar/internal/message/answer"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/validation"
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
		errAnswer = query.HandleQuery(socket, msg)
	case message.RPCTypeAnswer:
		errAnswer = jsonrpc.HandleAnswer(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	if errAnswer != nil {
		return errAnswer.Wrap("handleMessage")
	}

	return nil
}
