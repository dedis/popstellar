package reaction

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messageData/root"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
	"strings"
)

type Handler struct {
	subs   repository.SubscriptionManager
	db     repository.ReactionRepository
	schema *validation.SchemaValidator
}

func New(subs repository.SubscriptionManager, db repository.ReactionRepository,
	schema *validation.SchemaValidator) *Handler {
	return &Handler{
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (h *Handler) Handle(channelPath string, msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	laoPath, _ := strings.CutSuffix(channelPath, root.Social+root.Reactions)
	isAttendee, err := h.db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		return err
	}
	if !isAttendee {
		return errors.NewAccessDeniedError("user not inside roll-call")
	}

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		err = h.handleReactionAdd(msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
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

func (h *Handler) handleReactionAdd(msg message.Message) error {
	var reactMsg messagedata.ReactionAdd
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

func (h *Handler) handleReactionDelete(msg message.Message) error {
	var delReactMsg messagedata.ReactionDelete
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
