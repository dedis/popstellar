package catchup

import (
	"encoding/json"
	"popstellar/internal/repository"

	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
)

type Handler struct {
	db repository.CatchupRepository
}

func New(db repository.CatchupRepository) *Handler {
	return &Handler{
		db: db,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var catchup method.Catchup

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
