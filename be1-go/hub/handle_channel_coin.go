package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelCoin(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object#action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		errAnswer = handleCoinPostTransaction(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelCoin")
		return errAnswer
	}
	return nil
}

func handleCoinPostTransaction(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
