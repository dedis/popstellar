package channel

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/exp/slices"
	"popstellar/crypto"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strconv"
	"strings"
)

const (
	rollCallFlag    = "R"
	electionFlag    = "Election"
	questionFlag    = "Question"
	pluralityMethod = "Plurality"
	approvalMethod  = "Approval"
	open            = "open"
	closed          = "closed"
	created         = "created"
)

func handleChannelLao(params types.HandlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	storeMessage := true
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
		storeMessage = false
		errAnswer = handleRollCallClose(msg, channel, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		errAnswer = handleRollCallCreate(msg, channel, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		errAnswer = handleRollCallOpen(msg, channel, params)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		errAnswer = handleRollCallReOpen(msg, channel, params)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		storeMessage = false
		errAnswer = handleElectionSetup(msg, channel, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	if storeMessage {
		err := params.DB.StoreMessage(channel, msg)
		if err != nil {
			errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
			errAnswer = errAnswer.Wrap("handleChannelLao")
			return errAnswer
		}
	}
	return nil
}

func handleLaoState(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
	var laoState messagedata.LaoState
	err := msg.UnmarshalData(&laoState)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}
	ok, err := params.DB.HasMessage(laoState.ModificationID)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get check if message exists: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("message corresponding to modificationID %s does not exist", laoState.ModificationID)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	witnesses, err := params.DB.GetLaoWitnesses(channel)
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
	if errAnswer != nil {
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

func handleRollCallCreate(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
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

	// verify proposed start after creation
	if rollCallCreate.ProposedStart < rollCallCreate.Creation {
		errAnswer = answer.NewInvalidMessageFieldError("roll call proposed start time should be greater than creation time")
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

func handleRollCallOpen(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
	var rollCallOpen messagedata.RollCallOpen
	err := msg.UnmarshalData(&rollCallOpen)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(rollCallOpen.UpdateID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(channel, messagedata.RootPrefix, ""),
		rollCallOpen.Opens,
		strconv.Itoa(int(rollCallOpen.OpenedAt)),
	)
	if rollCallOpen.UpdateID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call update id is %s, should be %s", rollCallOpen.UpdateID, expectedID)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(rollCallOpen.Opens)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call opens: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	if rollCallOpen.OpenedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call opened at is %d, should be minimum 0", rollCallOpen.OpenedAt)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}
	ok, err := params.DB.CheckPrevID(channel, rollCallOpen.Opens)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to check if previous id exists: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("previous id does not exist")
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}
	return nil
}

func handleRollCallReOpen(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
	var rollCallReOpen messagedata.RollCallReOpen
	err := msg.UnmarshalData(&rollCallReOpen)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallReOpen")
		return errAnswer
	}

	errAnswer = handleRollCallOpen(msg, channel, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleRollCallReOpen")
		return errAnswer
	}

	return nil
}

func handleRollCallClose(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
	var rollCallClose messagedata.RollCallClose
	err := msg.UnmarshalData(&rollCallClose)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(rollCallClose.UpdateID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call update ID: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(channel, messagedata.RootPrefix, ""),
		rollCallClose.Closes,
		strconv.Itoa(int(rollCallClose.ClosedAt)),
	)
	if rollCallClose.UpdateID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("roll call update id is %s, should be %s", rollCallClose.UpdateID, expectedID)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(rollCallClose.Closes)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode roll call closes: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	if rollCallClose.ClosedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("roll call closed at is %d, should be minimum 0", rollCallClose.ClosedAt)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	state, err := params.DB.GetRollCallState(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get roll call state: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	} else if state != open {
		errAnswer = answer.NewInvalidMessageFieldError("roll call is not open")
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	channels := make([]string, 0, len(rollCallClose.Attendees))

	for _, popToken := range rollCallClose.Attendees {
		_, err = base64.URLEncoding.DecodeString(popToken)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to decode poptoken: %v", err)
			errAnswer = errAnswer.Wrap("handleRollCallClose")
			return errAnswer
		}
		chirpingChannelPath := channel + social + "/" + popToken
		channels = append(channels, chirpingChannelPath)
	}

	err = params.DB.StoreChannelsAndMessage(channels, channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store channels and message: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}
	return nil
}

func handleElectionSetup(msg message.Message, channel string, params types.HandlerParameters) *answer.Error {
	var electionSetup messagedata.ElectionSetup
	err := msg.UnmarshalData(&electionSetup)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode sender public key: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal sender public key: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	organizePubKey, err := params.DB.GetOrganizerPubKey(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get organizer public key: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	if !organizePubKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender public key does not match organizer public key: %s != %s", senderPubKey, organizePubKey)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(electionSetup.Lao)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode lao: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	if electionSetup.Lao != channel {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is %s, should be %s", electionSetup.Lao, channel)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(electionSetup.ID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode election id: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	// verify election setup message id
	expectedID := messagedata.Hash(
		electionFlag,
		channel,
		strconv.Itoa(int(electionSetup.CreatedAt)),
		electionSetup.Name,
	)
	if electionSetup.ID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is %s, should be %s", electionSetup.ID, expectedID)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if len(electionSetup.Name) == 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election name is empty")
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if electionSetup.Version != messagedata.OpenBallot && electionSetup.Version != messagedata.SecretBallot {
		errAnswer = answer.NewInvalidMessageFieldError("election version is %s, should be %s or %s", electionSetup.Version, messagedata.OpenBallot, messagedata.SecretBallot)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if electionSetup.CreatedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election created at is %d, should be minimum 0", electionSetup.CreatedAt)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if electionSetup.StartTime < electionSetup.CreatedAt {
		errAnswer = answer.NewInvalidMessageFieldError("election start should be greater that creation time")
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if electionSetup.EndTime < electionSetup.StartTime {
		errAnswer = answer.NewInvalidMessageFieldError("election end should be greater that start time")
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	if len(electionSetup.Questions) == 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election contains no questions")
		errAnswer = errAnswer.Wrap("handleElectionSetup")
	}

	for _, question := range electionSetup.Questions {
		_, err = base64.URLEncoding.DecodeString(question.ID)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to decode Question id: %v", err)
			errAnswer = errAnswer.Wrap("handleElectionSetup")
			return errAnswer
		}
		expectedID := messagedata.Hash(
			questionFlag,
			electionSetup.ID,
			question.Question,
		)
		if question.ID != expectedID {
			errAnswer = answer.NewInvalidMessageFieldError("Question id is %s, should be %s", question.ID, expectedID)
			errAnswer = errAnswer.Wrap("handleElectionSetup")
			return errAnswer
		}
		if len(question.Question) == 0 {
			errAnswer = answer.NewInvalidMessageFieldError("Question is empty")
			errAnswer = errAnswer.Wrap("handleElectionSetup")
			return errAnswer
		}
		if question.VotingMethod != pluralityMethod && question.VotingMethod != approvalMethod {
			errAnswer = answer.NewInvalidMessageFieldError("Question voting method is %s, should be %s or %s", question.VotingMethod, pluralityMethod, approvalMethod)
			errAnswer = errAnswer.Wrap("handleElectionSetup")
			return errAnswer
		}
	}
	electionPubKey, electionSecretKey := generateKeys()
	electionKeyMsg, errAnswer := createElectionKey(params, electionSetup.ID, electionPubKey)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	electionPath := channel + "/" + electionSetup.ID
	err = params.DB.StoreMessageWithElectionKey(channel, electionPath, msg.MessageID, electionPubKey, electionSecretKey, msg, electionKeyMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store election setup message: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	subs, ok := state.GetSubsInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleGreetServer")
		return errAnswer
	}

	subs.AddChannel(electionPath)

	err = params.DB.StoreMessage(electionSetup.ID, electionKeyMsg)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to store election key message: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return errAnswer
	}
	return nil
}

func createElectionKey(params types.HandlerParameters, electionID string, electionPubKey kyber.Point) (message.Message, *answer.Error) {
	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal election public key: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}
	msgData := messagedata.ElectionKey{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionKey,
		Election: electionID,
		Key:      base64.URLEncoding.EncodeToString(electionPubBuf),
	}

	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal message data: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}
	newData64 := base64.URLEncoding.EncodeToString(dataBuf)
	//TODO get server public key
	serverPubBuf, err := params.DB.GetServerPubKey()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to get server public key: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}
	signatureBuf, errAnswer := Sign(dataBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}
	signature := base64.URLEncoding.EncodeToString(signatureBuf)
	electionKeyMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}
	return electionKeyMsg, nil
}

// Not implemented yet
func handleLaoUpdate(msg message.Message, params types.HandlerParameters) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingCreate(msg message.Message, params types.HandlerParameters) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingState(msg message.Message, params types.HandlerParameters) *answer.Error {
	return nil
}
