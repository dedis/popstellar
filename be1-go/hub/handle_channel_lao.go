package hub

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/exp/slices"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strconv"
	"strings"
)

const (
	rollCallFlag = "R"
)

func handleChannelLao(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
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
		errAnswer = handleRollCallCreate(msg, channel, params)
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
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	err := params.db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelLao")
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
		errAnswer = errAnswer.Wrap("handleLaoState")
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

	// Check if the signatures match
	expected := len(witnesses)
	match := 0
	for _, modificationSignature := range laoState.ModificationSignatures {
		err = schnorr.VerifyWithChecks(crypto.Suite, []byte(modificationSignature.Witness),
			[]byte(laoState.ModificationID), []byte(modificationSignature.Signature))
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to verify signature for witness: %s", modificationSignature.Witness)
			errAnswer = errAnswer.Wrap("handleLaoState")
			return errAnswer
		}
		if _, ok := witnesses[modificationSignature.Witness]; ok {
			match++
		}
	}

	if match != expected {
		errAnswer = answer.NewInvalidMessageFieldError("not enough witness signatures provided. Needed %d got %d", expected, match)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	var updateMsgData messagedata.LaoUpdate

	err = msg.UnmarshalData(&updateMsgData)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal update message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	err = updateMsgData.Verify()
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify update message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
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

	numUpdateWitnesses := len(update.Witnesses)
	numStateWitnesses := len(state.Witnesses)

	if numUpdateWitnesses != numStateWitnesses {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between witness count")
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
		return errAnswer
	}

	match := 0
	for _, updateWitness := range update.Witnesses {
		if slices.Contains(state.Witnesses, updateWitness) {
			match++
		}
	}

	if match != numUpdateWitnesses {
		errAnswer = answer.NewInvalidMessageFieldError("mismatch between witness keys")
		errAnswer = errAnswer.Wrap("compareLaoUpdateAndState")
		return errAnswer
	}
	return nil
}

func handleRollCallCreate(msg message.Message, channel string, params handlerParameters) *answer.Error {
	var rollCallCreate messagedata.RollCallCreate
	err := msg.UnmarshalData(&rollCallCreate)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(rollCallCreate.ID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call ID: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify roll call create message id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(channel, messagedata.RootPrefix, ""),
		strconv.Itoa(int(rollCallCreate.Creation)),
		rollCallCreate.Name,
	)
	if rollCallCreate.ID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call id is %s, should be %s", rollCallCreate.ID, expectedID)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify creation is positive
	if rollCallCreate.Creation < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call creation is %d, should be minimum 0", rollCallCreate.Creation)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify proposed start after creation and proposed end after creation
	if rollCallCreate.ProposedStart < rollCallCreate.Creation || rollCallCreate.ProposedEnd < rollCallCreate.Creation {
		errAnswer = answer.NewInvalidMessageFieldError("roll call proposed start and proposed end should ve greater than creation")
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	// verify proposed end after proposed start
	if rollCallCreate.ProposedEnd < rollCallCreate.ProposedStart {
		errAnswer = answer.NewInvalidMessageFieldError("roll call proposed end should be greater than proposed start")
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	return nil
}

func handleRollCallClose(msg message.Message, params handlerParameters) *answer.Error {
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
