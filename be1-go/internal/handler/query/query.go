package query

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message/query"
	"popstellar/internal/network/socket"
)

type MethodHandler interface {
	Handle(socket socket.Socket, msg []byte) (*int, error)
}

type MethodHandlers struct {
	catchup         MethodHandler
	getmessagesbyid MethodHandler
	greetserver     MethodHandler
	heartbeat       MethodHandler
	publish         MethodHandler
	subscribe       MethodHandler
	unsubscribe     MethodHandler
	rumor           MethodHandler
}

type Handler struct {
	handlers MethodHandlers
}

func New(handlers MethodHandlers) *Handler {
	return &Handler{
		handlers: handlers,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) error {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	var id *int = nil

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, err = h.handlers.catchup.Handle(socket, msg)
	case query.MethodGetMessagesById:
		id, err = h.handlers.getmessagesbyid.Handle(socket, msg)
	case query.MethodGreetServer:
		_, err = h.handlers.greetserver.Handle(socket, msg)
	case query.MethodHeartbeat:
		_, err = h.handlers.heartbeat.Handle(socket, msg)
	case query.MethodPublish:
		id, err = h.handlers.publish.Handle(socket, msg)
	case query.MethodSubscribe:
		id, err = h.handlers.subscribe.Handle(socket, msg)
	case query.MethodUnsubscribe:
		id, err = h.handlers.unsubscribe.Handle(socket, msg)
	case query.MethodRumor:
		id, err = h.handlers.rumor.Handle(socket, msg)
	default:
		err = errors.NewInvalidActionError("unexpected method: '%s'", queryBase.Method)
	}

	if err != nil && id != nil {
		socket.SendPopError(id, err)
	}

	return err
}
