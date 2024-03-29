package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelChirp(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelChirp")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		errAnswer = handleChirpAdd(msg, params)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
		errAnswer = handleChirpDelete(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelChirp")
	}
	return nil
}

func handleChirpAdd(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleChirpDelete(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
