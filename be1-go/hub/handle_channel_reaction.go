package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelReaction(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelReaction")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		errAnswer = handleReactionAdd(params, msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		errAnswer = handleReactionDelete(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelReaction")
	}
	return nil

}

func handleReactionAdd(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleReactionDelete(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}
