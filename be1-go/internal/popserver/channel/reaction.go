package channel

import (
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelReaction(params types.HandlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		errAnswer = handleReactionAdd(params, msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		errAnswer = handleReactionDelete(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}

	err := params.DB.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}
	return nil

}

func handleReactionAdd(params types.HandlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleReactionDelete(params types.HandlerParameters, msg message.Message) *answer.Error {
	return nil
}
