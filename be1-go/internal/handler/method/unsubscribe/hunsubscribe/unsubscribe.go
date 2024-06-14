package hunsubscribe

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/network/socket"
)

type Subscribers interface {
	Unsubscribe(channel string, socket socket.Socket) error
}

type Handler struct {
	subs Subscribers
	log  zerolog.Logger
}

func New(subs Subscribers, log zerolog.Logger) *Handler {
	return &Handler{
		subs: subs,
		log:  log.With().Str("module", "unsubscribe").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var unsubscribe munsubscribe.Unsubscribe
	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	if channel.Root == unsubscribe.Params.Channel {
		return &unsubscribe.ID, errors.NewAccessDeniedError("cannot Unsubscribe from root channel")
	}

	err = h.subs.Unsubscribe(unsubscribe.Params.Channel, socket)
	if err != nil {
		return &unsubscribe.ID, err
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}
