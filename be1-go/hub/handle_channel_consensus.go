package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelConsensus(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelConsensus")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionElect:
		errAnswer = handleConsensusElect(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionElectAccept:
		errAnswer = handleConsensusDelete(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPrepare:
		errAnswer = handleConsensusPrepare(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPromise:
		errAnswer = handleConsensusPromise(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPropose:
		errAnswer = handleConsensusPropose(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionAccept:
		errAnswer = handleConsensusAccept(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionLearn:
		errAnswer = handleConsensusLearn(params, msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionFailure:
		errAnswer = handleConsensusFailure(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelConsensus")
		return errAnswer
	}
	return nil
}

func handleConsensusElect(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusDelete(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPrepare(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPromise(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPropose(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusAccept(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusLearn(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}

func handleConsensusFailure(params handlerParameters, msg message.Message) *answer.Error {
	return nil
}
