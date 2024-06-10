package hsubscribe

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/method/subscribe/msubscribe"
	"popstellar/internal/network/socket"
)

type Subscribers interface {
	Subscribe(channel string, socket socket.Socket) error
}

type Handler struct {
	subs Subscribers
}

func New(subs Subscribers) *Handler {
	return &Handler{subs: subs}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var subscribe msubscribe.Subscribe
	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	if messagedata.Root == subscribe.Params.Channel {
		return &subscribe.ID, errors.NewAccessDeniedError("cannot Subscribe to root channel")
	}

	err = h.subs.Subscribe(subscribe.Params.Channel, socket)
	if err != nil {
		return &subscribe.ID, err
	}

	socket.SendResult(subscribe.ID, nil, nil)

	return &subscribe.ID, nil
}
