package hub

import (
	"fmt"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelLao(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyMessageAndGetObjectAction(params, msg)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err).Wrap("handleChannelLao")
	}

	var errAnswer *answer.Error
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionState:
		errAnswer = handleLaoState(msg, params)
	case messagedata.LAOObject + "#" + messagedata.LAOActionUpdate:
		errAnswer = handleLaoUpdate(msg, params)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionCreate:
		errAnswer = handleMeetingCreate(msg, params)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionState:
		errAnswer = handleMeetingState(msg, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionClose:
		errAnswer = handleRollCallClose(msg, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		errAnswer = handleRollCallCreate(msg, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		errAnswer = handleRollCallOpen(msg, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		errAnswer = handleRollCallReOpen(msg, params)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		errAnswer = handleElectionSetup(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("invalid object and action")
	}
	if errAnswer != nil {
		return errAnswer.Wrap(fmt.Sprintf("failed to handle %s#%s", object, action)).Wrap("handleChannelLao")
	}
	return nil
}

func handleLaoState(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleLaoUpdate(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingCreate(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingState(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleRollCallClose(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleRollCallCreate(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleRollCallOpen(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleRollCallReOpen(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}

func handleElectionSetup(msg message.Message, params handlerParameters) *answer.Error {
	return nil
}
