package hanswer

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"math/rand"
	"popstellar/internal/errors"
	"popstellar/internal/handler/answer/manswer"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/network/socket"
	"sort"
)

const (
	maxRetry          = 10
	continueMongering = 0.5
)

type Queries interface {
	Remove(id int)
	GetRumor(queryID int) (mrumor.Rumor, bool)
	IsGetMessagesByID(id int) bool
	IsRumor(id int) bool
	IsRumorState(id int) bool
}

type MessageHandler interface {
	Handle(channelPath string, msg mmessage.Message, fromRumor bool) error
}

type RumorHandler interface {
	HandleRumorStateAnswer(rumor mrumor.ParamsRumor) error
	SendRumor(socket socket.Socket, rumor mrumor.Rumor)
}

type Handlers struct {
	MessageHandler MessageHandler
	RumorHandler   RumorHandler
}

type Handler struct {
	queries  Queries
	handlers Handlers
	log      zerolog.Logger
}

func New(queries Queries, handlers Handlers, log zerolog.Logger) *Handler {
	return &Handler{
		queries:  queries,
		handlers: handlers,
		log:      log.With().Str("module", "answer").Logger(),
	}
}

func (h *Handler) Handle(msg []byte) error {
	var answerMsg manswer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	if answerMsg.ID == nil {
		return errors.NewInvalidMessageFieldError("received an answer with a null id")
	}

	if h.queries.IsGetMessagesByID(*answerMsg.ID) {
		return h.handleGetMessagesByIDAnswer(answerMsg)
	}

	if h.queries.IsRumor(*answerMsg.ID) {
		return h.handleRumorAnswer(answerMsg)
	}

	if h.queries.IsRumorState(*answerMsg.ID) {
		return h.handleRumorStateAnswer(answerMsg)
	}

	return errors.NewInvalidActionError("received a invalid jsonrpc answer")
}

func (h *Handler) handleGetMessagesByIDAnswer(msg manswer.Answer) error {
	defer h.queries.Remove(*msg.ID)

	if msg.Result == nil {
		h.log.Info().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}

	if msg.Result.IsEmpty() {
		h.log.Info().Msg("expected isn't an answer to a query, nothing to handle")
		return nil
	}

	result := msg.Result.GetMessagesByChannel()
	msgsByChan := make(map[string]map[string]mmessage.Message)

	// Unmarshal each message
	for channelID, rawMsgs := range result {
		msgsByChan[channelID] = make(map[string]mmessage.Message)
		for _, rawMsg := range rawMsgs {
			var msg mmessage.Message
			err := json.Unmarshal(rawMsg, &msg)
			if err == nil {
				msgsByChan[channelID][msg.MessageID] = msg
				continue
			}

			err = errors.NewJsonUnmarshalError(err.Error())
			h.log.Error().Err(err)
		}

		if len(msgsByChan[channelID]) == 0 {
			delete(msgsByChan, channelID)
		}
	}

	// Handle every message and discard them if handled without error
	h.handleMessagesByChannel(msgsByChan)

	return nil
}

func (h *Handler) handleMessagesByChannel(msgsByChannel map[string]map[string]mmessage.Message) {
	// Handle every messages
	for i := 0; i < maxRetry; i++ {
		// Sort by channelID length
		sortedChannelIDs := h.getSortedChannels(msgsByChannel)

		h.tryToHandleMessages(msgsByChannel, sortedChannelIDs)

		if len(msgsByChannel) == 0 {
			return
		}
	}
}

func (h *Handler) tryToHandleMessages(msgsByChannel map[string]map[string]mmessage.Message, sortedChannelIDs []string) {
	for _, channelID := range sortedChannelIDs {
		msgs := msgsByChannel[channelID]
		for msgID, msg := range msgs {
			err := h.handlers.MessageHandler.Handle(channelID, msg, false)
			if err == nil {
				delete(msgsByChannel[channelID], msgID)
				continue
			}

			h.log.Error().Err(err)
		}

		if len(msgsByChannel[channelID]) == 0 {
			delete(msgsByChannel, channelID)
		}
	}
}

func (h *Handler) getSortedChannels(msgsByChannel map[string]map[string]mmessage.Message) []string {
	sortedChannelIDs := make([]string, 0)
	for channelID := range msgsByChannel {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})
	return sortedChannelIDs
}

func (h *Handler) handleRumorAnswer(msg manswer.Answer) error {
	defer h.queries.Remove(*msg.ID)

	h.log.Debug().Msgf("received an answer to rumor query %d", *msg.ID)

	if msg.Error != nil {
		h.log.Debug().Msgf("received an answer error to rumor query %d", *msg.ID)
		if msg.Error.Code != errors.DuplicateResourceErrorCode {
			h.log.Debug().Msgf("invalid error code to rumor query %d", *msg.ID)
			return nil
		}

		stop := rand.Float64() < continueMongering

		if stop {
			h.log.Debug().Msgf("stop mongering rumor query %d", *msg.ID)
			return nil
		}

		h.log.Debug().Msgf("continue mongering rumor query %d", *msg.ID)
	}

	h.log.Debug().Msgf("sender rumor need to continue sending query %d", *msg.ID)
	rumor, ok := h.queries.GetRumor(*msg.ID)
	if !ok {
		return errors.NewInternalServerError("rumor query %d doesn't exist", *msg.ID)
	}

	h.handlers.RumorHandler.SendRumor(nil, rumor)

	return nil
}

func (h *Handler) handleRumorStateAnswer(msg manswer.Answer) error {
	defer h.queries.Remove(*msg.ID)

	if msg.Result == nil {
		return nil
	}

	if msg.Result.IsEmpty() {
		return nil
	}

	rumors := make([]mrumor.ParamsRumor, 0)

	result := msg.Result.GetData()
	for _, rawRumor := range result {
		var rumor mrumor.ParamsRumor
		err := json.Unmarshal(rawRumor, &rumor)
		if err == nil {
			rumors = append(rumors, rumor)
			continue
		}

		err = errors.NewJsonUnmarshalError(err.Error())
		h.log.Error().Err(err)
	}

	sort.Slice(rumors, func(i, j int) bool {
		return rumors[i].Timestamp.IsBefore(rumors[j].Timestamp)
	})

	for _, rumor := range rumors {
		err := h.handlers.RumorHandler.HandleRumorStateAnswer(rumor)
		if err != nil {
			h.log.Error().Err(err).Msg("")
		}
	}

	return nil
}
