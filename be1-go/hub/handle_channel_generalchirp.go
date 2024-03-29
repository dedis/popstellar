package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelGeneralChirp(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelGeneralChirp")
	}
	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyAdd:
		errAnswer = handleChirpNotifyAdd(msg, params)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyDelete:
		errAnswer = handleChirpNotifyDelete(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelGeneralChirp")
	}
	return nil
}

func handleChirpNotifyAdd(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleChirpNotifyDelete(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
