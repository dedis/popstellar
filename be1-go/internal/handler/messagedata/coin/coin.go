package coin

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
)

type Repository interface {
	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg message.Message) error
}

type Handler struct {
	subs   repository.SubscriptionManager
	db     Repository
	schema *validation.SchemaValidator
}

func New(subs repository.SubscriptionManager, db Repository,
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

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		err = h.handleCoinPostTransaction(msg)
	default:
		err = errors.NewInvalidActionError("failed to Handle %s#%s, invalid object#action", object, action)
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

func (h *Handler) handleCoinPostTransaction(msg message.Message) error {
	var data messagedata.PostTransaction
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return data.Verify()
}
