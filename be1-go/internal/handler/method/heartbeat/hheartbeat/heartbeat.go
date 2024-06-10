package hheartbeat

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/message/method/mgetmessagesbyid"
	method2 "popstellar/internal/message/method/mheartbeat"
	"popstellar/internal/message/mquery"
	"popstellar/internal/network/socket"
)

type Queries interface {
	GetNextID() int
	AddQuery(ID int, query mgetmessagesbyid.GetMessagesById)
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
	var heartbeat method2.Heartbeat
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

	getMessagesById := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGetMessagesById,
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
