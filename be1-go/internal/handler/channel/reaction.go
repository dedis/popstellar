package channel

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
	"strings"
)

type reactionHandler struct {
	subs   repository.SubscriptionManager
	db     repository.ReactionRepository
	schema *validation.SchemaValidator
}

func createReactionHandler(subs repository.SubscriptionManager, db repository.ReactionRepository,
	schema *validation.SchemaValidator) *reactionHandler {
	return &reactionHandler{
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (r *reactionHandler) handle(channelPath string, msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = r.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	laoPath, _ := strings.CutSuffix(channelPath, Social+Reactions)
	isAttendee, err := r.db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		return err
	}
	if !isAttendee {
		return errors.NewAccessDeniedError("user not inside roll-call")
	}

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		err = r.handleReactionAdd(msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		err = r.handleReactionDelete(msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	err = r.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return r.subs.BroadcastToAllClients(msg, channelPath)
}

func (r *reactionHandler) handleReactionAdd(msg message.Message) error {
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

func (r *reactionHandler) handleReactionDelete(msg message.Message) error {
	var delReactMsg messagedata.ReactionDelete
	err := msg.UnmarshalData(&delReactMsg)
	if err != nil {
		return err
	}

	err = delReactMsg.Verify()
	if err != nil {
		return err
	}

	reactSender, err := r.db.GetReactionSender(delReactMsg.ReactionID)
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
