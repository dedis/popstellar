package hheartbeat

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
)

type Queries interface {
	GetNextID() int
	AddGetMessagesByID(ID int) error
}

type Repository interface {
	// GetParamsForGetMessageByID returns the params to do the getMessageByID msg in reponse of heartbeat
	GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error)
}

type Handler struct {
	queries Queries
	db      Repository
	log     zerolog.Logger
}

func New(queries Queries, db Repository, log zerolog.Logger) *Handler {
	return &Handler{
		queries: queries,
		db:      db,
		log:     log.With().Str("module", "heartbeat").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, byteMessage []byte) (*int, error) {
	var heartbeat mheartbeat.Heartbeat
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

	return nil, h.queries.AddGetMessagesByID(queryId)
}
