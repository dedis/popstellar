package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelElection(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelElection")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		errAnswer = handleVoteCastVote(msg, params)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		errAnswer = handleElectionOpen(msg, params)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		errAnswer = handleElectionEnd(msg, params)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionResult:
		errAnswer = handleElectionResult(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelElection")
	}
	return nil
}

func handleVoteCastVote(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleElectionOpen(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleElectionEnd(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleElectionResult(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
