package hrumorstate

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumorstate/mrumorstate"
	"popstellar/internal/network/socket"
	"sort"
)

type Repository interface {
	GetRumorTimestamp() (mrumor.RumorTimestamp, error)
	GetAllRumors() ([]mrumor.Rumor, error)
}

type Handler struct {
	db  Repository
	log zerolog.Logger
}

func New(db Repository, log zerolog.Logger) *Handler {
	return &Handler{
		db:  db,
		log: log.With().Str("module", "rumorstate").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var rumorState mrumorstate.RumorState
	err := json.Unmarshal(msg, &rumorState)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	myRumors, err := h.db.GetAllRumors()
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
