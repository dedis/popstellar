package query

import (
	"encoding/json"
	"fmt"
	"popstellar/internal/errors"
	"popstellar/internal/message/query"
	"popstellar/internal/network/socket"
)

type MethodHandler interface {
	Handle(socket socket.Socket, msg []byte) (*int, error)
}

type MethodHandlers struct {
	Catchup         MethodHandler
	GetMessagesbyid MethodHandler
	Greetserver     MethodHandler
	Heartbeat       MethodHandler
	Publish         MethodHandler
	Subscribe       MethodHandler
	Unsubscribe     MethodHandler
	Rumor           MethodHandler
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

	fmt.Println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy42")

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	var id *int = nil

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, err = h.handlers.Catchup.Handle(socket, msg)
	case query.MethodGetMessagesById:
		id, err = h.handlers.GetMessagesbyid.Handle(socket, msg)
	case query.MethodGreetServer:
		_, err = h.handlers.Greetserver.Handle(socket, msg)
	case query.MethodHeartbeat:
		_, err = h.handlers.Heartbeat.Handle(socket, msg)
	case query.MethodPublish:
		id, err = h.handlers.Publish.Handle(socket, msg)
	case query.MethodSubscribe:
		id, err = h.handlers.Subscribe.Handle(socket, msg)
	case query.MethodUnsubscribe:
		id, err = h.handlers.Unsubscribe.Handle(socket, msg)
	case query.MethodRumor:
		id, err = h.handlers.Rumor.Handle(socket, msg)
	default:
		err = errors.NewInvalidActionError("unexpected method: '%s'", queryBase.Method)
	}

	fmt.Println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy69")

	if err != nil && id != nil {
		socket.SendPopError(id, err)
	}

	return err
}
