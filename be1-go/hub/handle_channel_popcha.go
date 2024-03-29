package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelPopCha(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelPopCha")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.AuthObject + "#" + messagedata.AuthAction:
		errAnswer = handleAuth(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelPopCha")
	}
	return nil
}

func handleAuth(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}
