package hub

import (
	"fmt"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelLao(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionState:
		errAnswer = handleLaoState(msg, channel, params)
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

func handleLaoState(msg message.Message, channel string, params handlerParameters) *answer.Error {
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
	witnesses, err := params.db.GetLaoWitnesses(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get lao witnesses: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}
	expected := len(witnesses)
	match := 0
	for j := 0; j < len(laoState.ModificationSignatures); j++ {
		_, ok := witnesses[laoState.ModificationSignatures[j].Witness]
		if ok {
			match++
		}
	}

	if match != expected {
		errAnswer = answer.NewInvalidMessageFieldError("not enough witness signatures provided. Needed %d got %d", expected, match)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	// Check if the signatures match
	for _, pair := range laoState.ModificationSignatures {
		err := schnorr.VerifyWithChecks(crypto.Suite, []byte(pair.Witness),
			[]byte(laoState.ModificationID), []byte(pair.Signature))
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to verify signature for witness: %s", pair.Witness)
			errAnswer = errAnswer.Wrap("handleLaoState")
			return errAnswer
		}
	}
	var updateMsgData messagedata.LaoUpdate

	err := msg.UnmarshalData(&updateMsgData)
	if err != nil {
		return &answer.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal message from the inbox: %v", err),
		}
	}

	err = updateMsgData.Verify()
	if err != nil {
		return &answer.Error{
			Code:        -4,
			Description: fmt.Sprintf("invalid lao#update message: %v", err),
		}
	}

	errAnswer = compareLaoUpdateAndState(updateMsgData, laoState)
	if err != nil {
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	return nil
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) *answer.Error {
	var errAnswer *answer.Error
	if update.LastModified != state.LastModified {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified)
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
		return errAnswer
	}

	if update.Name != state.Name {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between name: expected %s got %s", update.Name, state.Name)
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between witness count: expected %d got %d", M, N)
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
		return errAnswer
	}

	match := 0

	for i := 0; i < M; i++ {
		for j := 0; j < N; j++ {
			if update.Witnesses[i] == state.Witnesses[j] {
				match++
				break
			}
		}
	}

	if match != M {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between witness keys: expected %d keys to match but %d matched", M, match)
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
		return errAnswer
	}

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
