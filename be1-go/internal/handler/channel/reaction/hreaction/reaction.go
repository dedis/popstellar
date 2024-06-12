package hreaction

import (
	"encoding/base64"
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/reaction/mreaction"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/validation"
	"strings"
)

type Subscribers interface {
	BroadcastToAllClients(msg mmessage.Message, channel string) error
}

type Repository interface {
	// IsAttendee returns if the user has participated in the last roll-call from the LAO
	IsAttendee(laoPath string, poptoken string) (bool, error)

	// GetReactionSender returns a reaction sender
	GetReactionSender(messageID string) (string, error)

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error
}

type Handler struct {
	subs   Subscribers
	db     Repository
	schema *validation.SchemaValidator
	log    zerolog.Logger
}

func New(subs Subscribers, db Repository, schema *validation.SchemaValidator, log zerolog.Logger) *Handler {
	return &Handler{
		subs:   subs,
		db:     db,
		schema: schema,
		log:    log.With().Str("module", "reaction").Logger(),
	}
}

func (h *Handler) Handle(channelPath string, msg mmessage.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	laoPath, _ := strings.CutSuffix(channelPath, channel.Reactions)
	isAttendee, err := h.db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		return err
	}
	if !isAttendee {
		return errors.NewAccessDeniedError("user not inside roll-call")
	}

	switch object + "#" + action {
	case channel.ReactionObject + "#" + channel.ReactionActionAdd:
		err = h.handleReactionAdd(msg)
	case channel.ReactionObject + "#" + channel.ReactionActionDelete:
		err = h.handleReactionDelete(msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	err = h.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) handleReactionAdd(msg mmessage.Message) error {
	var reactMsg mreaction.ReactionAdd
	err := msg.UnmarshalData(&reactMsg)
	if err != nil {
		return err
	}

	err = reactMsg.Verify()
	if err != nil {
		return err
	}

	return nil
}

func (h *Handler) handleReactionDelete(msg mmessage.Message) error {
	var delReactMsg mreaction.ReactionDelete
	err := msg.UnmarshalData(&delReactMsg)
	if err != nil {
		return err
	}

	err = delReactMsg.Verify()
	if err != nil {
		return err
	}

	reactSender, err := h.db.GetReactionSender(delReactMsg.ReactionID)
	if err != nil {
		return err
	}
	if reactSender == "" {
		return errors.NewInvalidResourceError("unknown reaction")
	}

	if msg.Sender != reactSender {
		return errors.NewAccessDeniedError("only the owner of the reaction can delete it")
	}

	return nil
}
