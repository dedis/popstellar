package hcatchup

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/method/mcatchup"
	"popstellar/internal/network/socket"
)

type Repository interface {
	// GetAllMessagesFromChannel return all the messages received + sent on a channel
	GetAllMessagesFromChannel(channelID string) ([]mmessage.Message, error)
}

type Handler struct {
	db Repository
}

func New(db Repository) *Handler {
	return &Handler{
		db: db,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var catchup mcatchup.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	result, err := h.db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		return &catchup.ID, err
	}

	socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}
