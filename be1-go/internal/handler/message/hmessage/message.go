package hmessage

import (
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
)

type Repository interface {

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// GetChannelType returns the type of the channel.
	GetChannelType(channel string) (string, error)
}

type ChannelHandler interface {
	Handle(channelPath string, msg mmessage.Message) error
}

type ChannelHandlers map[string]ChannelHandler

type Handler struct {
	db       Repository
	handlers ChannelHandlers
	log      zerolog.Logger
}

func New(db Repository, handlers ChannelHandlers, log zerolog.Logger) *Handler {
	return &Handler{
		db:       db,
		handlers: handlers,
		log:      log.With().Str("module", "message").Logger(),
	}
}

func (h *Handler) Handle(channelPath string, msg mmessage.Message, fromRumor bool) error {
	err := msg.VerifyMessage()
	if err != nil {
		return err
	}

	msgAlreadyExists, err := h.db.HasMessage(msg.MessageID)
	if err != nil {
		return err
	}
	if msgAlreadyExists && fromRumor {
		return nil
	}
	if msgAlreadyExists {
		return errors.NewDuplicateResourceError("message %s was already received", msg.MessageID)
	}

	channelType, err := h.db.GetChannelType(channelPath)
	if err != nil {
		return err
	}

	handler, ok := h.handlers[channelType]
	if !ok {
		return errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}

	return handler.Handle(channelPath, msg)
}
