package hjsonrpc

import (
	"popstellar/internal/errors"
	"popstellar/internal/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/validation"
)

type QueryHandler interface {
	Handle(socket socket.Socket, msg []byte) error
}

type AnswerHandler interface {
	Handle(msg []byte) error
}

type Handler struct {
	schema        *validation.SchemaValidator
	queryHandler  QueryHandler
	answerHandler AnswerHandler
}

func New(schema *validation.SchemaValidator, queryHandler QueryHandler, answerHandler AnswerHandler) *Handler {
	return &Handler{
		schema:        schema,
		queryHandler:  queryHandler,
		answerHandler: answerHandler,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) error {
	err := h.schema.VerifyJSON(msg, validation.GenericMessage)
	if err != nil {
		return err
	}

	rpcType, err := message.GetType(msg)
	if err != nil {
		return err
	}

	switch rpcType {
	case message.RPCTypeQuery:
		err = h.queryHandler.Handle(socket, msg)

	case message.RPCTypeAnswer:
		err = h.answerHandler.Handle(msg)
	default:
		err = errors.NewInvalidMessageFieldError("jsonRPC is of unknown type")
	}

	return err
}
