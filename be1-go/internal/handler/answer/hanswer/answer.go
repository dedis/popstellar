package hanswer

import (
	"encoding/json"
	"math/rand"
	"popstellar/internal/errors"
	"popstellar/internal/logger"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
	"sort"
)

const (
	maxRetry          = 10
	continueMongering = 0.5
)

type Queries interface {
	SetQueryReceived(ID int) error
	IsRumorQuery(queryID int) bool
	GetRumorFromPastQuery(queryID int) (method.Rumor, bool)
}

type MessageHandler interface {
	Handle(channelPath string, msg message.Message, fromRumor bool) error
}

type RumorSender interface {
	SendRumor(socket socket.Socket, rumor method.Rumor)
}

type Handlers struct {
	MessageHandler MessageHandler
	RumorSender    RumorSender
}

type Handler struct {
	queries  Queries
	handlers Handlers
}

func New(queries Queries, handlers Handlers) *Handler {
	return &Handler{
		queries:  queries,
		handlers: handlers,
	}
}

func (h *Handler) Handle(msg []byte) error {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	if answerMsg.ID == nil {
		logger.Logger.Info().Msg("received an answer with a null id")
		return nil
	}

	isRumor := h.queries.IsRumorQuery(*answerMsg.ID)
	if isRumor {
		return h.handleRumorAnswer(answerMsg)
	}

	if answerMsg.Result == nil {
		logger.Logger.Info().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}

	if answerMsg.Result.IsEmpty() {
		logger.Logger.Info().Msg("expected isn't an answer to a popquery, nothing to handle")
		return nil
	}

	err = h.queries.SetQueryReceived(*answerMsg.ID)
	if err != nil {
		return err
	}

	h.handleGetMessagesByIDAnswer(answerMsg)

	return nil
}

func (h *Handler) handleRumorAnswer(msg answer.Answer) error {
	err := h.queries.SetQueryReceived(*msg.ID)
	if err != nil {
		return err
	}

	logger.Logger.Debug().Msgf("received an answer to rumor query %d", *msg.ID)

	if msg.Error != nil {
		logger.Logger.Debug().Msgf("received an answer error to rumor query %d", *msg.ID)
		if msg.Error.Code != errors.DuplicateResourceErrorCode {
			logger.Logger.Debug().Msgf("invalid error code to rumor query %d", *msg.ID)
			return nil
		}

		stop := rand.Float64() < continueMongering

		if stop {
			logger.Logger.Debug().Msgf("stop mongering rumor query %d", *msg.ID)
			return nil
		}

		logger.Logger.Debug().Msgf("continue mongering rumor query %d", *msg.ID)
	}

	logger.Logger.Debug().Msgf("sender rumor need to continue sending query %d", *msg.ID)
	rumor, ok := h.queries.GetRumorFromPastQuery(*msg.ID)
	if !ok {
		return errors.NewInternalServerError("rumor query %d doesn't exist", *msg.ID)
	}

	h.handlers.RumorSender.SendRumor(nil, rumor)

	return nil
}

func (h *Handler) handleGetMessagesByIDAnswer(msg answer.Answer) {
	result := msg.Result.GetMessagesByChannel()
	msgsByChan := make(map[string]map[string]message.Message)

	// Unmarshal each message
	for channelID, rawMsgs := range result {
		msgsByChan[channelID] = make(map[string]message.Message)
		for _, rawMsg := range rawMsgs {
			var msg message.Message
			err := json.Unmarshal(rawMsg, &msg)
			if err == nil {
				msgsByChan[channelID][msg.MessageID] = msg
				continue
			}

			err = errors.NewJsonUnmarshalError(err.Error())
			logger.Logger.Error().Err(err)
		}

		if len(msgsByChan[channelID]) == 0 {
			delete(msgsByChan, channelID)
		}
	}

	// Handle every message and discard them if handled without error
	h.handleMessagesByChannel(msgsByChan)
}

func (h *Handler) handleMessagesByChannel(msgsByChannel map[string]map[string]message.Message) {
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

func (h *Handler) tryToHandleMessages(msgsByChannel map[string]map[string]message.Message, sortedChannelIDs []string) {
	for _, channelID := range sortedChannelIDs {
		msgs := msgsByChannel[channelID]
		for msgID, msg := range msgs {
			err := h.handlers.MessageHandler.Handle(channelID, msg, false)
			if err == nil {
				delete(msgsByChannel[channelID], msgID)
				continue
			}

			logger.Logger.Error().Err(err)
		}

		if len(msgsByChannel[channelID]) == 0 {
			delete(msgsByChannel, channelID)
		}
	}
}

func (h *Handler) getSortedChannels(msgsByChannel map[string]map[string]message.Message) []string {
	sortedChannelIDs := make([]string, 0)
	for channelID := range msgsByChannel {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})
	return sortedChannelIDs
}
