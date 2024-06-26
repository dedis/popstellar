package hrumorstate

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumorstate/mrumorstate"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"sort"
)

type Repository interface {
	GetRumorTimestamp() (mrumor.RumorTimestamp, error)
	GetAllRumorParams() ([]mrumor.ParamsRumor, error)
}

type Queries interface {
	GetNextID() int
	AddRumorState(id int) error
}

type Sockets interface {
	SendToRandom(buf []byte)
}

type Handler struct {
	queries Queries
	sockets Sockets
	db      Repository
	log     zerolog.Logger
}

func New(queries Queries, sockets Sockets, db Repository, log zerolog.Logger) *Handler {
	return &Handler{
		queries: queries,
		sockets: sockets,
		db:      db,
		log:     log.With().Str("module", "rumor_state").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var rumorState mrumorstate.RumorState
	err := json.Unmarshal(msg, &rumorState)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	myParams, err := h.db.GetAllRumorParams()
	if err != nil {
		return nil, err
	}
	params := make([]mrumor.ParamsRumor, 0)

	for _, param := range myParams {
		rumorID, ok := rumorState.Params.State[param.SenderID]
		if !ok || rumorID < param.RumorID {
			params = append(params, param)
		}
	}

	sort.Slice(params, func(i, j int) bool {
		return params[i].Timestamp.IsBefore(params[j].Timestamp)
	})

	socket.SendRumorStateAnswer(rumorState.ID, params)

	return nil, nil
}

func (h *Handler) SendRumorState() error {
	timestamp, err := h.db.GetRumorTimestamp()
	if err != nil {
		return err
	}

	id := h.queries.GetNextID()

	rumorStateMessage := mrumorstate.RumorState{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "rumor_state",
		},
		ID: id,
		Params: mrumorstate.RumorStateParams{
			State: timestamp,
		},
	}

	buf, err := json.Marshal(rumorStateMessage)
	if err != nil {
		return errors.NewJsonMarshalError(err.Error())
	}

	err = h.queries.AddRumorState(id)
	if err != nil {
		return err
	}

	h.sockets.SendToRandom(buf)
	return nil
}
