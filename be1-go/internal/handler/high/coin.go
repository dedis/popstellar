package high

import (
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
)

func handleChannelCoin(channelPath string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelCoin")
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		errAnswer = handleCoinPostTransaction(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelCoin")
	}

	db, errAnswer := database.GetCoinRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelCoin")
	}

	err := db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleChannelCoin")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelCoin")
	}

	return nil
}

func handleCoinPostTransaction(msg message.Message) *answer.Error {
	var data messagedata.PostTransaction

	errAnswer := msg.UnmarshalMsgData(&data)
	if errAnswer != nil {
		return errAnswer.Wrap("handleCoinPostTransaction")
	}

	err := data.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid data: %v", err)
		return errAnswer.Wrap("handleCoinPostTransaction")
	}

	return nil
}
