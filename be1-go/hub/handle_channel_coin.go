package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelCoin(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelCoin")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		errAnswer = handleCoinPostTransaction(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelCoin")
	}
	return nil
}

func handleCoinPostTransaction(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
