package hmessage

import (
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
)

type Repository interface {

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// GetChannelType returns the type of the oldchannel.
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
}

func New(db Repository, handlers DataHandlers) *Handler {
	return &Handler{
		db:       db,
		handlers: handlers,
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
	case messagedata.RootObject:
		err = h.handlers.Root.Handle(channelPath, msg)
	case messagedata.LAOObject:
		err = h.handlers.Lao.Handle(channelPath, msg)
	case messagedata.ElectionObject:
		err = h.handlers.Election.Handle(channelPath, msg)
	case messagedata.ChirpObject:
		err = h.handlers.Chirp.Handle(channelPath, msg)
	case messagedata.ReactionObject:
		err = h.handlers.Reaction.Handle(channelPath, msg)
	case messagedata.CoinObject:
		err = h.handlers.Coin.Handle(channelPath, msg)
	case messagedata.FederationObject:
		err = h.handlers.Federation.Handle(channelPath, msg)
	default:
		err = errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}
	return err
}
