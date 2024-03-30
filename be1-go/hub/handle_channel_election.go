package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelElection(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object#action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}

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
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
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
