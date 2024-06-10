package hunsubscribe

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/network/socket"
)

type Subscribers interface {
	Unsubscribe(channel string, socket socket.Socket) error
}

type Handler struct {
	subs Subscribers
}

func New(subs Subscribers) *Handler {
	return &Handler{subs: subs}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var unsubscribe munsubscribe.Unsubscribe
	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	if messagedata.Root == unsubscribe.Params.Channel {
		return &unsubscribe.ID, errors.NewAccessDeniedError("cannot Unsubscribe from root channel")
	}

	err = h.subs.Unsubscribe(unsubscribe.Params.Channel, socket)
	if err != nil {
		return &unsubscribe.ID, err
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}
