package message

import (
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
)

const (
	RootType         = "root"
	LaoType          = "lao"
	ElectionType     = "election"
	ChirpType        = "chirp"
	ReactionType     = "reaction"
	ConsensusType    = "consensus"
	CoinType         = "coin"
	AuthType         = "auth"
	PopChaType       = "popcha"
	GeneralChirpType = "generalChirp"
	FederationType   = "federation"
)

type MessageDataHandler interface {
	Handle(channelPath string, msg message.Message) error
}

type MessageDataHandlers struct {
	Root       MessageDataHandler
	Lao        MessageDataHandler
	Election   MessageDataHandler
	Chirp      MessageDataHandler
	Reaction   MessageDataHandler
	Coin       MessageDataHandler
	Federation MessageDataHandler
}

type Handler struct {
	db       repository.ChannelRepository
	handlers MessageDataHandlers
}

func New(db repository.Repository, handlers MessageDataHandlers) *Handler {
	return &Handler{
		db:       db,
		handlers: handlers,
	}
}

func (h *Handler) Handle(channelPath string, msg message.Message, fromRumor bool) error {
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
	case RootType:
		err = h.handlers.Root.Handle(channelPath, msg)
	case LaoType:
		err = h.handlers.Lao.Handle(channelPath, msg)
	case ElectionType:
		err = h.handlers.Election.Handle(channelPath, msg)
	case ChirpType:
		err = h.handlers.Chirp.Handle(channelPath, msg)
	case ReactionType:
		err = h.handlers.Reaction.Handle(channelPath, msg)
	case CoinType:
		err = h.handlers.Coin.Handle(channelPath, msg)
	case FederationType:
		err = h.handlers.Federation.Handle(channelPath, msg)
	default:
		err = errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}
	return err
}
