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
	GetAllRumors() ([]mrumor.Rumor, error)
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

	myRumors, err := h.db.GetAllRumors()
	if err != nil {
		return &rumorState.ID, err
	}

	rumors := make([]mrumor.Rumor, 0)

	for _, rumor := range myRumors {
		if rumor.Params.Timestamp.IsBefore(rumorState.Params.State) {
			continue
		}
		rumors = append(rumors, rumor)
	}

	sort.Slice(rumors, func(i, j int) bool {
		return rumors[i].Params.Timestamp.IsBefore(rumors[j].Params.Timestamp)
	})

	socket.SendRumorStateAnswer(rumorState.ID, rumors)

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
