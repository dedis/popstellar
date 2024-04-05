package hub

import (
	"encoding/base64"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/validation"
)

func handleChannelCoin(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		errAnswer = handleCoinPostTransaction(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	err := params.db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, params, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	return nil
}

func handleCoinPostTransaction(params handlerParameters, msg message.Message) *answer.Error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err).
			Wrap("handleCoinPostTransaction")
		return errAnswer
	}

	err = params.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify json schema: %w", err).
			Wrap("handleCoinPostTransaction")
		return errAnswer
	}

	var data messagedata.PostTransaction

	err = msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).
			Wrap("handleCoinPostTransaction")
		return errAnswer
	}

	err = data.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid data: %v", err).
			Wrap("handleCoinPostTransaction")
		return errAnswer
	}

	return nil
}
