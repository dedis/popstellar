package hcoin

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/messagedata/mcoin"
	"popstellar/internal/validation"
)

type Subscribers interface {
	BroadcastToAllClients(msg mmessage.Message, channel string) error
}

type Repository interface {
	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error
}

type Handler struct {
	subs   Subscribers
	db     Repository
	schema *validation.SchemaValidator
}

func New(subs Subscribers, db Repository,
	schema *validation.SchemaValidator) *Handler {
	return &Handler{
		subs:   subs,
		db:     db,
		schema: schema,
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

	object, action, err := mmessage.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case mmessage.CoinObject + "#" + mmessage.CoinActionPostTransaction:
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

func (h *Handler) handleCoinPostTransaction(msg mmessage.Message) error {
	var data mcoin.PostTransaction
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return data.Verify()
}
