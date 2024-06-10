package hpublish

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/logger"
	"popstellar/internal/message/method/mpublish"
	"popstellar/internal/message/mmessage"
	"popstellar/internal/network/socket"
	"strings"
)

const thresholdMessagesByRumor = 1

type Hub interface {
	NotifyResetRumorSender() error
}

type Repository interface {
	// AddMessageToMyRumor adds the message to the last rumor of the server and returns the current number of message inside the last rumor
	AddMessageToMyRumor(messageID string) (int, error)
}

type MessageHandler interface {
	Handle(channelPath string, msg mmessage.Message, fromRumor bool) error
}

type Handler struct {
	hub            Hub
	db             Repository
	messageHandler MessageHandler
}

func New(hub Hub, db Repository, messageHandler MessageHandler) *Handler {
	return &Handler{
		hub:            hub,
		db:             db,
		messageHandler: messageHandler,
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var publish mpublish.Publish
	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	err = h.messageHandler.Handle(publish.Params.Channel, publish.Params.Message, false)
	if err != nil {
		return &publish.ID, err
	}

	socket.SendResult(publish.ID, nil, nil)

	if strings.Contains(publish.Params.Channel, "federation") {
		return nil, nil
	}

	logger.Logger.Debug().Msgf("sender rumor need to add message %s", publish.Params.Message.MessageID)
	nbMessagesInsideRumor, err := h.db.AddMessageToMyRumor(publish.Params.Message.MessageID)
	if err != nil {
		return nil, err
	}

	if nbMessagesInsideRumor < thresholdMessagesByRumor {
		logger.Logger.Debug().Msgf("no enough message to send rumor %s", publish.Params.Message.MessageID)
		return nil, nil
	}

	return nil, h.hub.NotifyResetRumorSender()
}
