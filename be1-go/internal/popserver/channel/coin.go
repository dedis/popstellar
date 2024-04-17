package channel

import (
	"popstellar/internal/popserver/singleton/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelCoin(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		errAnswer = handleCoinPostTransaction(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	db, ok := database.GetCoinRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelCoin")
		return errAnswer
	}

	err := db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	return nil
}

func handleCoinPostTransaction(msg message.Message) *answer.Error {
	var data messagedata.PostTransaction

	err := msg.UnmarshalData(&data)
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
