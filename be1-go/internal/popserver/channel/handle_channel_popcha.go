package channel

import (
	"popstellar/internal/popserver/state"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelPopCha(params state.HandlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
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

	err := params.DB.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
		return errAnswer
	}
	return nil
}

func handleAuth(params state.HandlerParameters, msg message.Message) *answer.Error {
	return nil
}
