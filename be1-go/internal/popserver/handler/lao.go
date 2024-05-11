package handler

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
	"strings"
)

func handleChannelLao(channelPath string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelLao")
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionState:
		errAnswer = handleLaoState(msg, channelPath)
	case messagedata.LAOObject + "#" + messagedata.LAOActionUpdate:
		errAnswer = handleLaoUpdate(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionCreate:
		errAnswer = handleMeetingCreate(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionState:
		errAnswer = handleMeetingState(msg)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionClose:
		storeMessage = false
		errAnswer = handleRollCallClose(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		errAnswer = handleRollCallCreate(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		errAnswer = handleRollCallOpen(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		errAnswer = handleRollCallReOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		storeMessage = false
		errAnswer = handleElectionSetup(msg, channelPath)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelLao")
	}

	if storeMessage {
		db, errAnswer := database.GetLAORepositoryInstance()
		if errAnswer != nil {
			return errAnswer.Wrap("handleChannelLao")
		}

		err := db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			errAnswer = answer.NewStoreDatabaseError("message: %v", err)
			return errAnswer.Wrap("handleChannelLao")
		}
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelLao")
	}
	return nil
}

func handleRollCallCreate(msg message.Message, channelPath string) *answer.Error {
	var rollCallCreate messagedata.RollCallCreate
	errAnswer := msg.UnmarshalMsgData(&rollCallCreate)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallCreate")
	}

	errAnswer = rollCallCreate.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallCreate")
	}

	return nil
}

func handleRollCallOpen(msg message.Message, channelPath string) *answer.Error {
	var rollCallOpen messagedata.RollCallOpen
	errAnswer := msg.UnmarshalMsgData(&rollCallOpen)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallOpen")
	}

	errAnswer = rollCallOpen.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallOpen")
	}

	db, errAnswer := database.GetLAORepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallOpen")
	}

	ok, err := db.CheckPrevID(channelPath, rollCallOpen.Opens, messagedata.RollCallActionCreate)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError("if previous id exists: %v", err)
		return errAnswer.Wrap("handleRollCallOpen")
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("previous id does not exist")
		return errAnswer.Wrap("handleRollCallOpen")
	}
	return nil
}

func handleRollCallReOpen(msg message.Message, channelPath string) *answer.Error {
	var rollCallReOpen messagedata.RollCallReOpen
	errAnswer := msg.UnmarshalMsgData(&rollCallReOpen)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallReOpen")
	}

	errAnswer = handleRollCallOpen(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallReOpen")
	}

	return nil
}

func handleRollCallClose(msg message.Message, channelPath string) *answer.Error {
	var rollCallClose messagedata.RollCallClose
	errAnswer := msg.UnmarshalMsgData(&rollCallClose)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallClose")
	}

	errAnswer = rollCallClose.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallClose")
	}

	db, errAnswer := database.GetLAORepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleRollCallClose")
	}

	ok, err := db.CheckPrevID(channelPath, rollCallClose.Closes, messagedata.RollCallActionOpen)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError("if previous id exists: %v", err)
		return errAnswer.Wrap("handleRollCallClose")
	} else if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("previous id does not exist")
		return errAnswer.Wrap("handleRollCallClose")
	}

	channels := make([]string, 0, len(rollCallClose.Attendees))

	for _, popToken := range rollCallClose.Attendees {
		_, err = base64.URLEncoding.DecodeString(popToken)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to decode poptoken: %v", err)
			return errAnswer.Wrap("handleRollCallClose")
		}
		chirpingChannelPath := channelPath + Social + "/" + popToken
		channels = append(channels, chirpingChannelPath)
	}

	for _, channelPath := range channels {
		errAnswer := state.AddChannel(channelPath)
		if errAnswer != nil {
			return errAnswer.Wrap("handleRollCallClose")
		}
	}

	err = db.StoreRollCallClose(channels, channelPath, msg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError("channels and message: %v", err)
		return errAnswer.Wrap("handleRollCallClose")
	}

	return nil
}

func handleElectionSetup(msg message.Message, channelPath string) *answer.Error {
	var electionSetup messagedata.ElectionSetup
	errAnswer := msg.UnmarshalMsgData(&electionSetup)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionSetup")
	}

	errAnswer = verifySenderLao(channelPath, msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionSetup")
	}

	laoID, _ := strings.CutPrefix(channelPath, RootPrefix)

	errAnswer = electionSetup.Verify(laoID)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionSetup")
	}

	for _, question := range electionSetup.Questions {
		errAnswer = question.Verify(electionSetup.ID)
		if errAnswer != nil {
			return errAnswer.Wrap("handleElectionSetup")
		}
	}

	errAnswer = storeElection(msg, electionSetup, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionSetup")
	}
	return nil
}

func verifySenderLao(channelPath string, msg message.Message) *answer.Error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode sender public key: %v", err)
		return errAnswer
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal sender public key: %v", err)
		return errAnswer
	}

	db, errAnswer := database.GetLAORepositoryInstance()
	if errAnswer != nil {
		return errAnswer
	}

	organizePubKey, err := db.GetOrganizerPubKey(channelPath)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError("organizer public key: %v", err)
		return errAnswer
	}

	if !organizePubKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender public key does not match organizer public key: %s != %s",
			senderPubKey, organizePubKey)
		return errAnswer
	}

	return nil
}

func storeElection(msg message.Message, electionSetup messagedata.ElectionSetup, channelPath string) *answer.Error {
	var errAnswer *answer.Error

	electionPubKey, electionSecretKey := generateKeys()
	var electionKeyMsg message.Message
	electionPath := channelPath + "/" + electionSetup.ID

	db, errAnswer := database.GetLAORepositoryInstance()
	if errAnswer != nil {
		return errAnswer
	}

	if electionSetup.Version == messagedata.SecretBallot {
		electionKeyMsg, errAnswer = createElectionKey(electionSetup.ID, electionPubKey)
		if errAnswer != nil {
			return errAnswer
		}
		err := db.StoreElectionWithElectionKey(channelPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)
		if err != nil {
			errAnswer = answer.NewStoreDatabaseError("election setup message: %v", err)
			return errAnswer
		}
	} else {
		err := db.StoreElection(channelPath, electionPath, electionPubKey, electionSecretKey, msg)
		if err != nil {
			errAnswer = answer.NewStoreDatabaseError("election setup message: %v", err)
			return errAnswer
		}
	}

	errAnswer = state.AddChannel(electionPath)
	if errAnswer != nil {
		return errAnswer
	}

	return nil
}

func createElectionKey(electionID string, electionPubKey kyber.Point) (message.Message, *answer.Error) {
	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal election public key: %v", err)
		return message.Message{}, errAnswer.Wrap("createAndSendElectionKey")
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
		return message.Message{}, errAnswer.Wrap("createAndSendElectionKey")
	}
	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, errAnswer := config.GetServerPublicKeyInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendElectionKey")
	}

	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server secret key", err)
		return message.Message{}, errAnswer.Wrap("createAndSendElectionKey")
	}
	signatureBuf, errAnswer := Sign(dataBuf)
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendElectionKey")
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
func handleLaoState(msg message.Message, channelPath string) *answer.Error {
	var laoState messagedata.LaoState
	errAnswer := msg.UnmarshalMsgData(&laoState)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoState")
	}

	db, errAnswer := database.GetLAORepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoState")
	}

	ok, err := db.HasMessage(laoState.ModificationID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if message exists: %v", err)
		return errAnswer.Wrap("handleLaoState")
	} else if !ok {
		errAnswer := answer.NewInvalidMessageFieldError("message corresponding to modificationID %s does not exist", laoState.ModificationID)
		return errAnswer.Wrap("handleLaoState")
	}

	witnesses, err := db.GetLaoWitnesses(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("lao witnesses: %v", err)
		return errAnswer.Wrap("handleLaoState")
	}

	// Check if the signatures match
	expected := len(witnesses)
	match := 0
	for _, modificationSignature := range laoState.ModificationSignatures {
		err = schnorr.VerifyWithChecks(crypto.Suite, []byte(modificationSignature.Witness),
			[]byte(laoState.ModificationID), []byte(modificationSignature.Signature))
		if err != nil {
			errAnswer := answer.NewInvalidMessageFieldError("failed to verify signature for witness: %s", modificationSignature.Witness)
			return errAnswer.Wrap("handleLaoState")
		}
		if _, ok := witnesses[modificationSignature.Witness]; ok {
			match++
		}
	}

	if match != expected {
		errAnswer := answer.NewInvalidMessageFieldError("not enough witness signatures provided. Needed %d got %d", expected, match)
		return errAnswer.Wrap("handleLaoState")
	}

	var updateMsgData messagedata.LaoUpdate

	err = msg.UnmarshalData(&updateMsgData)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal update message data: %v", err)
		return errAnswer.Wrap("handleLaoState")
	}

	err = updateMsgData.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify update message data: %v", err)
		return errAnswer.Wrap("handleLaoState")
	}

	errAnswer = compareLaoUpdateAndState(updateMsgData, laoState)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoState")
	}
	return nil
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) *answer.Error {
	if update.LastModified != state.LastModified {
		errAnswer := answer.NewInvalidMessageFieldError("mismatch between last modified: expected %d got %d",
			update.LastModified, state.LastModified)
		return errAnswer.Wrap("compareLaoUpdateAndState")
	}

	if update.Name != state.Name {
		errAnswer := answer.NewInvalidMessageFieldError("mismatch between name: expected %s got %s",
			update.Name, state.Name)
		return errAnswer.Wrap("compareLaoUpdateAndState")
	}

	numUpdateWitnesses := len(update.Witnesses)
	numStateWitnesses := len(state.Witnesses)

	if numUpdateWitnesses != numStateWitnesses {
		errAnswer := answer.NewInvalidMessageFieldError("mismatch between witness count")
		return errAnswer.Wrap("compareLaoUpdateAndState")
	}

	match := 0
	for _, updateWitness := range update.Witnesses {
		if slices.Contains(state.Witnesses, updateWitness) {
			match++
		}
	}
	if match != numUpdateWitnesses {
		errAnswer := answer.NewInvalidMessageFieldError("mismatch between witness keys")
		return errAnswer.Wrap("compareLaoUpdateAndState")
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
