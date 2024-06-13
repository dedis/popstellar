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

// MethodHandlers is map with a handler per method (method : handler)
type MethodHandlers map[string]MethodHandler

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

	handler, ok := h.handlers[queryBase.Method]
	if !ok {
		return errors.NewInvalidActionError("unexpected method: '%s'", queryBase.Method)
	}

	id, err := handler.Handle(socket, msg)

	if err != nil && id != nil {
		socket.SendPopError(id, err)
	}

	return err
}
