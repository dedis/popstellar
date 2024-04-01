package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelPopCha(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.AuthObject + "#" + messagedata.AuthAction:
		errAnswer = handleAuth(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
		return errAnswer
	}
	return nil
}

func handleAuth(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}
