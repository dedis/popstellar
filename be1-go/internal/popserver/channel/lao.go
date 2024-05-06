package channel

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/exp/slices"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelLao(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionState:
		errAnswer = handleLaoState(msg, channel)
	case messagedata.LAOObject + "#" + messagedata.LAOActionUpdate:
		errAnswer = handleLaoUpdate(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionCreate:
		errAnswer = handleMeetingCreate(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionState:
		errAnswer = handleMeetingState(msg)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionClose:
		storeMessage = false
		errAnswer = handleRollCallClose(msg, channel)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		errAnswer = handleRollCallCreate(msg, channel)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		errAnswer = handleRollCallOpen(msg, channel)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		errAnswer = handleRollCallReOpen(msg, channel)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		storeMessage = false
		errAnswer = handleElectionSetup(msg, channel)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}

	if storeMessage {
		db, ok := database.GetLAORepositoryInstance()
		if !ok {
			errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelLao")
			return errAnswer
		}

		err := db.StoreMessageAndData(channel, msg)
		if err != nil {
			errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
			errAnswer = errAnswer.Wrap("handleChannelLao")
			return errAnswer
		}
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelLao")
		return errAnswer
	}
	return nil
}

func handleRollCallCreate(msg message.Message, channel string) *answer.Error {
	var rollCallCreate messagedata.RollCallCreate
	err := msg.UnmarshalData(&rollCallCreate)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	errAnswer = rollCallCreate.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleRollCallCreate")
		return errAnswer
	}

	return nil
}

func handleRollCallOpen(msg message.Message, channel string) *answer.Error {
	var rollCallOpen messagedata.RollCallOpen
	err := msg.UnmarshalData(&rollCallOpen)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	errAnswer = rollCallOpen.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	db, ok := database.GetLAORepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database")
		errAnswer = errAnswer.Wrap("handleRollCallOpen")
		return errAnswer
	}

	ok, err = db.CheckPrevID(channel, rollCallOpen.Opens, messagedata.RollCallActionCreate)

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

func handleRollCallReOpen(msg message.Message, channel string) *answer.Error {
	var rollCallReOpen messagedata.RollCallReOpen
	err := msg.UnmarshalData(&rollCallReOpen)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallReOpen")
		return errAnswer
	}

	errAnswer = handleRollCallOpen(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleRollCallReOpen")
		return errAnswer
	}

	return nil
}

func handleRollCallClose(msg message.Message, channel string) *answer.Error {
	var rollCallClose messagedata.RollCallClose
	err := msg.UnmarshalData(&rollCallClose)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	errAnswer = rollCallClose.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	db, ok := database.GetLAORepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database")
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}

	ok, err = db.CheckPrevID(channel, rollCallClose.Closes, messagedata.RollCallActionOpen)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to check if previous id exists: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("previous id does not exist")
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
		chirpingChannelPath := channel + Social + "/" + popToken
		channels = append(channels, chirpingChannelPath)
	}

	err = db.StoreChannelsAndMessage(channels, channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store channels and message: %v", err)
		errAnswer = errAnswer.Wrap("handleRollCallClose")
		return errAnswer
	}
	return nil
}

func handleElectionSetup(msg message.Message, channel string) *answer.Error {
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

	db, ok := database.GetLAORepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleElectionSetup")
		return errAnswer
	}

	organizePubKey, err := db.GetOrganizerPubKey(channel)
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

	errAnswer = electionSetup.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}

	for _, question := range electionSetup.Questions {
		errAnswer = question.Verify(electionSetup.ID)
		if errAnswer != nil {
			errAnswer = errAnswer.Wrap("handleElectionSetup")
			return errAnswer
		}
	}
	electionPubKey, electionSecretKey := generateKeys()
	electionKeyMsg, errAnswer := createElectionKey(electionSetup.ID, electionPubKey)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionSetup")
		return errAnswer
	}
	electionPath := channel + "/" + electionSetup.ID
	err = db.StoreMessageWithElectionKey(channel, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)
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
	return nil
}

func createElectionKey(electionID string, electionPubKey kyber.Point) (message.Message, *answer.Error) {
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

	serverPublicKey, ok := config.GetServerPublicKeyInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}

	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server secret key", err)
		errAnswer = errAnswer.Wrap("createAndSendElectionKey")
		return message.Message{}, errAnswer
	}
	signatureBuf, errAnswer := Sign(dataBuf)
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

// Not working
func handleLaoState(msg message.Message, channel string) *answer.Error {
	var laoState messagedata.LaoState
	err := msg.UnmarshalData(&laoState)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	db, ok := database.GetLAORepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleLaoState")
		return errAnswer
	}

	ok, err = db.HasMessage(laoState.ModificationID)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get check if message exists: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("message corresponding to modificationID %s does not exist", laoState.ModificationID)
		errAnswer = errAnswer.Wrap("handleLaoState")
		return errAnswer
	}

	witnesses, err := db.GetLaoWitnesses(channel)
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
		return errAnswer
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

// Not implemented yet
func handleLaoUpdate(msg message.Message) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingCreate(msg message.Message) *answer.Error {
	return nil
}

// Not implemented yet
func handleMeetingState(msg message.Message) *answer.Error {
	return nil
}
