package hub

import (
	"popstellar/internal/errors"
	jsonrpc "popstellar/internal/handler/answer"
	"popstellar/internal/handler/query"
	"popstellar/internal/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/validation"
)

func HandleIncomingMessage(socket socket.Socket, msg []byte) error {
	err := utils.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		return err
	}

	rpcType, err := message.GetType(msg)
	if err != nil {
		return err
	}

	switch rpcType {
	case message.RPCTypeQuery:
		err = query.HandleQuery(socket, msg)
	case message.RPCTypeAnswer:
		err = jsonrpc.HandleAnswer(msg)
	default:
		err = errors.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	return err
}
