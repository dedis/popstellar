package hquery

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/query/mquery"
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
	log      zerolog.Logger
}

func New(handlers MethodHandlers, log zerolog.Logger) *Handler {
	return &Handler{
		handlers: handlers,
		log:      log.With().Str("module", "query").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) error {
	var queryBase mquery.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	var id *int = nil

	switch queryBase.Method {
	case mquery.MethodCatchUp:
		id, err = h.handlers.Catchup.Handle(socket, msg)
	case mquery.MethodGetMessagesById:
		id, err = h.handlers.GetMessagesbyid.Handle(socket, msg)
	case mquery.MethodGreetServer:
		_, err = h.handlers.Greetserver.Handle(socket, msg)
	case mquery.MethodHeartbeat:
		_, err = h.handlers.Heartbeat.Handle(socket, msg)
	case mquery.MethodPublish:
		id, err = h.handlers.Publish.Handle(socket, msg)
	case mquery.MethodSubscribe:
		id, err = h.handlers.Subscribe.Handle(socket, msg)
	case mquery.MethodUnsubscribe:
		id, err = h.handlers.Unsubscribe.Handle(socket, msg)
	case mquery.MethodRumor:
		id, err = h.handlers.Rumor.Handle(socket, msg)
	default:
		err = errors.NewInvalidActionError("unexpected method: '%s'", queryBase.Method)
	}

	if err != nil && id != nil {
		socket.SendPopError(id, err)
	}

	return err
}
