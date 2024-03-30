package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelGeneralChirp(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object#action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyAdd:
		errAnswer = handleChirpNotifyAdd(msg, params)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyDelete:
		errAnswer = handleChirpNotifyDelete(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}
	return nil
}

func handleChirpNotifyAdd(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleChirpNotifyDelete(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
