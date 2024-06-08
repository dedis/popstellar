package getmessagesbyid

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
)

type Repository interface {
	GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error)
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
	var getMessagesById method.GetMessagesById
	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	result, err := h.db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		return &getMessagesById.ID, err
	}

	socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
