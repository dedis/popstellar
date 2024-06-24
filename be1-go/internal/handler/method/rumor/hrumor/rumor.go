package hrumor

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/network/socket"
	"sort"
)

const maxRetry = 10

type Queries interface {
	GetNextID() int
	AddRumor(id int, query mrumor.Rumor) error
}

type Sockets interface {
	SendRumor(socket socket.Socket, senderID string, rumorID int, buf []byte)
}

type Repository interface {
	// CheckRumor returns true if the rumor already exists
	CheckRumor(senderID string, rumorID int, timestamp mrumor.RumorTimestamp) (valid, alreadyHas bool, err error)

	// StoreRumor stores the new rumor with its processed and unprocessed messages
	StoreRumor(rumorID int, sender string, timestamp mrumor.RumorTimestamp, unprocessed map[string][]mmessage.Message, processed []string) error

	// GetUnprocessedMessagesByChannel returns all the unprocessed messages by channel
	GetUnprocessedMessagesByChannel() (map[string][]mmessage.Message, error)

	// GetRumorTimestamp returns the rumor state
	GetRumorTimestamp() (mrumor.RumorTimestamp, error)
}

type MessageHandler interface {
	Handle(channelPath string, msg mmessage.Message, fromRumor bool) error
}

type Handler struct {
	queries        Queries
	sockets        Sockets
	db             Repository
	messageHandler MessageHandler
	buf            *buffer
	log            zerolog.Logger
}

func New(queries Queries, sockets Sockets, db Repository, messageHandler MessageHandler, log zerolog.Logger) *Handler {
	return &Handler{
		queries:        queries,
		sockets:        sockets,
		db:             db,
		messageHandler: messageHandler,
		buf:            newBuffer(),
		log:            log.With().Str("module", "rumor").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var rumor mrumor.Rumor
	err := json.Unmarshal(msg, &rumor)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	h.log.Debug().Msgf("received rumor %s-%d from query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)

	ok, alreadyHas, err := h.db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID, rumor.Params.Timestamp)
	if err != nil {
		h.log.Error().Err(err)
		return &rumor.ID, err
	}
	if alreadyHas {
		return &rumor.ID, errors.NewDuplicateResourceError("rumor %s:%d already exists",
			rumor.Params.SenderID, rumor.Params.RumorID)
	}
	if !ok {
		h.log.Debug().Msgf("Trying to insert into buffer rumor %s:%d", rumor.Params.SenderID, rumor.Params.RumorID)
		err = h.buf.insert(rumor)
		if err != nil {
			return &rumor.ID, err
		}

		socket.SendResult(rumor.ID, nil, nil)

		return nil, nil
	}

	socket.SendResult(rumor.ID, nil, nil)

	return nil, h.handleAndPropagate(socket, rumor)
}

func (h *Handler) handleAndPropagate(socket socket.Socket, rumor mrumor.Rumor) error {
	h.SendRumor(socket, rumor)

	processedMsgs := h.tryHandlingMessagesByChannel(rumor.Params.Messages)

	err := h.db.StoreRumor(rumor.Params.RumorID, rumor.Params.SenderID, rumor.Params.Timestamp, rumor.Params.Messages, processedMsgs)
	if err != nil {
		return err
	}

	messages, err := h.db.GetUnprocessedMessagesByChannel()
	if err != nil {
		return err
	}

	_ = h.tryHandlingMessagesByChannel(messages)

	state, err := h.db.GetRumorTimestamp()
	if err != nil {
		return err
	}

	nextRumor, ok := h.buf.getNextRumor(state)
	if !ok {
		return nil
	}

	return h.handleNextRumor(nextRumor)
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

			h.log.Error().Err(err)
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

	err := h.queries.AddRumor(id, rumor)
	if err != nil {
		h.log.Error().Err(err)
		return
	}

	buf, err := json.Marshal(rumor)
	if err != nil {
		h.log.Error().Err(err)
		return
	}

	h.log.Debug().Msgf("sending rumor %s-%d query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)
	h.sockets.SendRumor(socket, rumor.Params.SenderID, rumor.Params.RumorID, buf)
}

func (h *Handler) handleNextRumor(rumor mrumor.Rumor) error {
	ok, _, err := h.db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID, rumor.Params.Timestamp)
	if err != nil {
		return err
	}
	if !ok {
		return nil
	}

	return h.handleAndPropagate(nil, rumor)
}

func (h *Handler) HandleRumorStateAnswer(rumor mrumor.Rumor) error {
	ok, alreadyHas, err := h.db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID, rumor.Params.Timestamp)
	if err != nil {
		return err
	}
	if alreadyHas {
		return errors.NewDuplicateResourceError("rumor %s:%d already exists", rumor.Params.SenderID, rumor.Params.RumorID)
	}
	if !ok {
		err = h.buf.insert(rumor)
		if err != nil {
			return err
		}

		return nil
	}

	return h.handleAndPropagate(nil, rumor)
}
