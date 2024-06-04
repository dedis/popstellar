package channel

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
)

type coinHandler struct {
	subs   repository.SubscriptionManager
	db     repository.CoinRepository
	schema *validation.SchemaValidator
}

func createCoinHandler(subs repository.SubscriptionManager, db repository.CoinRepository,
	schema *validation.SchemaValidator) *coinHandler {
	return &coinHandler{
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (c *coinHandler) handle(channelPath string, msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = c.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		err = c.handleCoinPostTransaction(msg)
	default:
		err = errors.NewInvalidActionError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	err = c.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return c.subs.BroadcastToAllClients(msg, channelPath)
}

func (c *coinHandler) handleCoinPostTransaction(msg message.Message) error {
	var data messagedata.PostTransaction
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return data.Verify()
}
