package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelConsensus(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelConsensus")
	}

	var errAnswer *answer.Error
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
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelConsensus")
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
