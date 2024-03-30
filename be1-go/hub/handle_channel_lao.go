package hub

import (
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelLao(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

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
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		err = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}
	return nil
}

func handleLaoState(msg message.Message, params handlerParameters) *answer.Error {
	var laoState messagedata.LaoState
	err := msg.UnmarshalData(&laoState)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	ok, err := params.db.HasMessage(laoState.ModificationID)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get check if message exists: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("message corresponding to modificationID %s does not exist", laoState.ModificationID)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}
	params.db.GetLaoWitnesses(laoState.ID)

	return nil
}

// Not implemented yet
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
