package channel

import (
	"popstellar/internal/popserver/singleton/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
)

func handleChannelConsensus(socket socket.Socket, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelConsensus")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionElect:
		errAnswer = handleConsensusElect(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionElectAccept:
		errAnswer = handleConsensusDelete(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPrepare:
		errAnswer = handleConsensusPrepare(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPromise:
		errAnswer = handleConsensusPromise(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionPropose:
		errAnswer = handleConsensusPropose(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionAccept:
		errAnswer = handleConsensusAccept(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionLearn:
		errAnswer = handleConsensusLearn(msg)
	case messagedata.ConsensusObject + "#" + messagedata.ConsensusActionFailure:
		errAnswer = handleConsensusFailure(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelConsensus")
		return errAnswer
	}

	db, ok := database.GetConsensusRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelConsensus")
		return errAnswer
	}

	err := db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelConsensus")
		return errAnswer
	}

	return nil
}

func handleConsensusElect(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusDelete(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPrepare(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPromise(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusPropose(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusAccept(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusLearn(msg message.Message) *answer.Error {
	return nil
}

func handleConsensusFailure(msg message.Message) *answer.Error {
	return nil
}
