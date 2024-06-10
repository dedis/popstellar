package hrumor

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/logger"
	"popstellar/internal/message/method/mrumor"
	"popstellar/internal/network/socket"
	"sort"
)

const maxRetry = 10

type Queries interface {
	GetNextID() int
	AddRumorQuery(id int, query mrumor.Rumor)
}

type Sockets interface {
	SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte)
}

type Repository interface {
	// CheckRumor returns true if the rumor already exists
	CheckRumor(senderID string, rumorID int) (bool, error)

	// StoreRumor stores the new rumor with its processed and unprocessed messages
	StoreRumor(rumorID int, sender string, unprocessed map[string][]mmessage.Message, processed []string) error

	// GetUnprocessedMessagesByChannel returns all the unprocessed messages by channel
	GetUnprocessedMessagesByChannel() (map[string][]mmessage.Message, error)
}

type MessageHandler interface {
	Handle(channelPath string, msg mmessage.Message, fromRumor bool) error
}

type Handler struct {
	queries        Queries
	sockets        Sockets
	db             Repository
	messageHandler MessageHandler
}

func New(queries Queries, sockets Sockets, db Repository,
	messageHandler MessageHandler) *Handler {
	return &Handler{
		queries:        queries,
		sockets:        sockets,
		db:             db,
		messageHandler: messageHandler,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var rumor mrumor.Rumor
	err := json.Unmarshal(msg, &rumor)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	logger.Logger.Debug().Msgf("received rumor %s-%d from query %d",
		rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)

	ok, err := h.db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID)
	if err != nil {
		return &rumor.ID, err
	}
	if !ok {
		return &rumor.ID, errors.NewDuplicateResourceError("rumor [%s|%v] is not valid",
			rumor.Params.SenderID, rumor.Params.RumorID)
	}

	socket.SendResult(rumor.ID, nil, nil)

	h.SendRumor(socket, rumor)

	processedMsgs := h.tryHandlingMessagesByChannel(rumor.Params.Messages)

	err = h.db.StoreRumor(rumor.Params.RumorID, rumor.Params.SenderID, rumor.Params.Messages, processedMsgs)
	if err != nil {
		return nil, err
	}

	messages, err := h.db.GetUnprocessedMessagesByChannel()
	if err != nil {
		return nil, err
	}

	_ = h.tryHandlingMessagesByChannel(messages)

	return nil, nil
}

func (h *Handler) tryHandlingMessagesByChannel(unprocessedMsgsByChannel map[string][]mmessage.Message) []string {
	processedMsgs := make([]string, 0)

	sortedChannels := h.sortChannels(unprocessedMsgsByChannel)

	for _, channelPath := range sortedChannels {
		unprocessedMsgs, newProcessedMsgs := h.tryHandlingMessages(channelPath, unprocessedMsgsByChannel[channelPath])

		if len(newProcessedMsgs) > 0 {
			processedMsgs = append(processedMsgs, newProcessedMsgs...)
		}

		if len(unprocessedMsgs) > 0 {
			unprocessedMsgsByChannel[channelPath] = unprocessedMsgs
		} else {
			delete(unprocessedMsgsByChannel, channelPath)
		}
	}

	return processedMsgs
}

func (h *Handler) tryHandlingMessages(channelPath string, unprocessedMsgs []mmessage.Message) ([]mmessage.Message, []string) {
	processedMsgs := make([]string, 0)

	for i := 0; i < maxRetry; i++ {
		nbProcessed := 0
		for index, msg := range unprocessedMsgs {
			err := h.messageHandler.Handle(channelPath, msg, true)
			if err == nil {
				unprocessedMsgs = h.removeMessage(index-nbProcessed, unprocessedMsgs)
				processedMsgs = append(processedMsgs, msg.MessageID)
				nbProcessed++
				continue
			}

			logger.Logger.Error().Err(err)
		}

		if len(unprocessedMsgs) == 0 {
			break
		}
	}

	return unprocessedMsgs, processedMsgs
}

func (h *Handler) removeMessage(index int, messages []mmessage.Message) []mmessage.Message {
	result := make([]mmessage.Message, 0)
	result = append(result, messages[:index]...)
	return append(result, messages[index+1:]...)
}

func (h *Handler) sortChannels(msgsByChannel map[string][]mmessage.Message) []string {
	sortedChannelIDs := make([]string, 0)
	for channelID := range msgsByChannel {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})
	return sortedChannelIDs
}

func (h *Handler) SendRumor(socket socket.Socket, rumor mrumor.Rumor) {
	id := h.queries.GetNextID()
	rumor.ID = id

	h.queries.AddRumorQuery(id, rumor)

	buf, err := json.Marshal(rumor)
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	logger.Logger.Debug().Msgf("sending rumor %s-%d query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)
	h.sockets.SendRumor(socket, rumor.Params.SenderID, rumor.Params.RumorID, buf)
}
