package hpublish

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/logger"
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

type FederationHandler interface {
	HandleWithSocket(channelPath string, msg mmessage.Message, socket socket.Socket) error
}

type Handler struct {
	hub               Hub
	db                Repository
	messageHandler    MessageHandler
	federationHandler FederationHandler
	log               zerolog.Logger
}

func New(hub Hub, db Repository, messageHandler MessageHandler,
	federationHandler FederationHandler, log zerolog.Logger) *Handler {
	return &Handler{
		hub:               hub,
		db:                db,
		messageHandler:    messageHandler,
		federationHandler: federationHandler,
		log:               log.With().Str("module", "publish").Logger(),
	}
}

func (h *Handler) Handle(socket socket.Socket, msg []byte) (*int, error) {
	var publish mpublish.Publish
	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	// The federation handler need to have access to the socket and are not
	// using rumors, except for tokens exchange
	if strings.Contains(publish.Params.Channel, channel.Federation) {
		err = h.federationHandler.HandleWithSocket(publish.Params.Channel, publish.Params.Message, socket)
	} else {
		err = h.messageHandler.Handle(publish.Params.Channel, publish.Params.Message, false)
	}

	if err != nil {
		return &publish.ID, err
	}

	socket.SendResult(publish.ID, nil, nil)

	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		return &publish.ID, errors.NewInvalidMessageFieldError(
			"failed to decode message data: %v", err)
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	if err != nil {
		return &publish.ID, err
	}

	if object == channel.FederationObject && action != channel.FederationActionTokensExchange {
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
