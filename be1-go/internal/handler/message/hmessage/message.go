package hmessage

import (
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/message/mmessage"
)

type Repository interface {

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// GetChannelType returns the type of the channel.
	GetChannelType(channel string) (string, error)
}

type DataHandler interface {
	Handle(channelPath string, msg mmessage.Message) error
}

type DataHandlers struct {
	Root       DataHandler
	Lao        DataHandler
	Election   DataHandler
	Chirp      DataHandler
	Reaction   DataHandler
	Coin       DataHandler
	Federation DataHandler
}

type Handler struct {
	db       Repository
	handlers DataHandlers
	log      zerolog.Logger
}

func New(db Repository, handlers DataHandlers, log zerolog.Logger) *Handler {
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

	switch channelType {
	case channel.RootObject:
		err = h.handlers.Root.Handle(channelPath, msg)
	case channel.LAOObject:
		err = h.handlers.Lao.Handle(channelPath, msg)
	case channel.ElectionObject:
		err = h.handlers.Election.Handle(channelPath, msg)
	case channel.ChirpObject:
		err = h.handlers.Chirp.Handle(channelPath, msg)
	case channel.ReactionObject:
		err = h.handlers.Reaction.Handle(channelPath, msg)
	case channel.CoinObject:
		err = h.handlers.Coin.Handle(channelPath, msg)
	case channel.FederationObject:
		err = h.handlers.Federation.Handle(channelPath, msg)
	default:
		err = errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}
	return err
}
