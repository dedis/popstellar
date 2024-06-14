package hgetmessagesbyid

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	"popstellar/internal/network/socket"
)

type Repository interface {
	GetResultForGetMessagesByID(params map[string][]string) (map[string][]mmessage.Message, error)
}

type Handler struct {
	db  Repository
	log zerolog.Logger
}

func New(db Repository, log zerolog.Logger) *Handler {
	return &Handler{
		db:  db,
		log: log.With().Str("module", "getmessagesbyid").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var getMessagesById mgetmessagesbyid.GetMessagesById
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
