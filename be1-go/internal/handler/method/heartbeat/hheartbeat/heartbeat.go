package hheartbeat

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
)

type Queries interface {
	GetNextID() int
	AddQuery(ID int, query method.GetMessagesById)
}

type Repository interface {
	// GetParamsForGetMessageByID returns the params to do the getMessageByID msg in reponse of heartbeat
	GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error)
}

type Handler struct {
	queries Queries
	db      Repository
}

func New(queries Queries, db Repository) *Handler {
	return &Handler{
		queries: queries,
		db:      db,
	}
}

func (h *Handler) Handle(socket socket.Socket, byteMessage []byte) (*int, error) {
	var heartbeat method.Heartbeat
	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	result, err := h.db.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	}

	queryId := h.queries.GetNextID()

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGetMessagesById,
		},
		ID:     queryId,
		Params: result,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		return nil, errors.NewJsonMarshalError(err.Error())
	}

	socket.Send(buf)

	h.queries.AddQuery(queryId, getMessagesById)

	return nil, nil
}
