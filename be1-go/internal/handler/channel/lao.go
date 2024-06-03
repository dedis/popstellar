package channel

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/exp/slices"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"strings"
)

func handleChannelLao(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionState:
		err = handleLaoState(msg, channelPath)
	case messagedata.LAOObject + "#" + messagedata.LAOActionUpdate:
		err = handleLaoUpdate(msg)
	case messagedata.MessageObject + "#" + messagedata.MessageActionWitness:
		err = handleMessageWitness(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionCreate:
		err = handleMeetingCreate(msg)
	case messagedata.MeetingObject + "#" + messagedata.MeetingActionState:
		err = handleMeetingState(msg)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionClose:
		storeMessage = false
		err = handleRollCallClose(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		err = handleRollCallCreate(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		err = handleRollCallOpen(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		err = handleRollCallReOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		storeMessage = false
		err = handleElectionSetup(msg, channelPath)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	if storeMessage {
		db, err := database.GetLAORepositoryInstance()
		if err != nil {
			return err
		}

		err = db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			return err
		}
	}

	return broadcastToAllClients(msg, channelPath)
}

func handleRollCallCreate(msg message.Message, channelPath string) error {
	var rollCallCreate messagedata.RollCallCreate
	err := msg.UnmarshalData(&rollCallCreate)
	if err != nil {
		return err
	}

	return rollCallCreate.Verify(channelPath)
}

func handleRollCallOpen(msg message.Message, channelPath string) error {
	var rollCallOpen messagedata.RollCallOpen
	err := msg.UnmarshalData(&rollCallOpen)
	if err != nil {
		return err
	}

	err = rollCallOpen.Verify(channelPath)
	if err != nil {
		return err
	}

	db, err := database.GetLAORepositoryInstance()
	if err != nil {
		return err
	}

	ok, err := db.CheckPrevCreateOrCloseID(channelPath, rollCallOpen.Opens)
	if err != nil {
		return err
	} else if !ok {
		return errors.NewInvalidMessageFieldError("previous id does not exist")
	}

	return nil
}

func handleRollCallReOpen(msg message.Message, channelPath string) error {
	var rollCallReOpen messagedata.RollCallReOpen
	err := msg.UnmarshalData(&rollCallReOpen)
	if err != nil {
		return err
	}

	return handleRollCallOpen(msg, channelPath)
}

func handleRollCallClose(msg message.Message, channelPath string) error {
	var rollCallClose messagedata.RollCallClose
	err := msg.UnmarshalData(&rollCallClose)
	if err != nil {
		return err
	}

	err = rollCallClose.Verify(channelPath)
	if err != nil {
		return err
	}

	db, err := database.GetLAORepositoryInstance()
	if err != nil {
		return err
	}

	ok, err := db.CheckPrevOpenOrReopenID(channelPath, rollCallClose.Closes)
	if err != nil {
		return err
	} else if !ok {
		return errors.NewInvalidMessageFieldError("previous id does not exist")
	}

	newChannels, err := createNewAttendeeChannels(channelPath, rollCallClose)
	if err != nil {
		return err
	}

	return db.StoreRollCallClose(newChannels, channelPath, msg)
}

func createNewAttendeeChannels(channelPath string, rollCallClose messagedata.RollCallClose) ([]string, error) {
	channels := make([]string, 0, len(rollCallClose.Attendees))

	for _, popToken := range rollCallClose.Attendees {
		_, err := base64.URLEncoding.DecodeString(popToken)
		if err != nil {
			return nil, errors.NewInvalidMessageFieldError("failed to decode poptoken: %v", err)
		}

		chirpingChannelPath := channelPath + Social + "/" + popToken
		channels = append(channels, chirpingChannelPath)
	}

	newChannels := make([]string, 0)
	for _, channelPath := range channels {
		alreadyExists, err := state.HasChannel(channelPath)
		if err != nil {
			return nil, err
		}
		if alreadyExists {
			continue
		}

		err = state.AddChannel(channelPath)
		if err != nil {
			return nil, err
		}

		newChannels = append(newChannels, channelPath)
	}

	return newChannels, nil
}

func handleElectionSetup(msg message.Message, channelPath string) error {
	var electionSetup messagedata.ElectionSetup
	err := msg.UnmarshalData(&electionSetup)
	if err != nil {
		return err
	}

	err = verifySenderLao(channelPath, msg)
	if err != nil {
		return err
	}

	laoID, _ := strings.CutPrefix(channelPath, RootPrefix)

	err = electionSetup.Verify(laoID)
	if err != nil {
		return err
	}

	for _, question := range electionSetup.Questions {
		err = question.Verify(electionSetup.ID)
		if err != nil {
			return err
		}
	}

	return storeElection(msg, electionSetup, channelPath)
}

func verifySenderLao(channelPath string, msg message.Message) error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode sender public key: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to unmarshal sender public key: %v", err)
	}

	db, err := database.GetLAORepositoryInstance()
	if err != nil {
		return err
	}

	organizePubKey, err := db.GetOrganizerPubKey(channelPath)
	if err != nil {
		return err
	}
	if !organizePubKey.Equal(senderPubKey) {
		return errors.NewAccessDeniedError("sender public key does not match organizer public key: %s != %s",
			senderPubKey, organizePubKey)
	}

	return nil
}

func storeElection(msg message.Message, electionSetup messagedata.ElectionSetup, channelPath string) error {
	electionPubKey, electionSecretKey := generateKeys()
	var electionKeyMsg message.Message
	electionPath := channelPath + "/" + electionSetup.ID

	db, err := database.GetLAORepositoryInstance()
	if err != nil {
		return err
	}

	if electionSetup.Version == messagedata.SecretBallot {
		electionKeyMsg, err = createElectionKey(electionSetup.ID, electionPubKey)
		if err != nil {
			return err
		}

		err = db.StoreElectionWithElectionKey(channelPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)
		if err != nil {
			return err
		}
	} else {
		err = db.StoreElection(channelPath, electionPath, electionPubKey, electionSecretKey, msg)
		if err != nil {
			return err
		}
	}

	return state.AddChannel(electionPath)
}

func createElectionKey(electionID string, electionPubKey kyber.Point) (message.Message, error) {
	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to marshal election public key: %v", err)
	}

	msgData := messagedata.ElectionKey{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionKey,
		Election: electionID,
		Key:      base64.URLEncoding.EncodeToString(electionPubBuf),
	}

	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}
	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, err := config.GetServerPublicKeyInstance()
	if err != nil {
		return message.Message{}, err
	}

	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to unmarshall server secret key", err)
	}

	signatureBuf, err := sign(dataBuf)
	if err != nil {
		return message.Message{}, err
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
func handleLaoState(msg message.Message, channelPath string) error {
	var laoState messagedata.LaoState
	err := msg.UnmarshalData(&laoState)
	if err != nil {
		return err
	}

	db, err := database.GetLAORepositoryInstance()
	if err != nil {
		return err
	}

	ok, err := db.HasMessage(laoState.ModificationID)
	if err != nil {
		return err
	} else if !ok {
		return errors.NewInvalidMessageFieldError("message corresponding to modificationID %s does not exist",
			laoState.ModificationID)
	}

	witnesses, err := db.GetLaoWitnesses(channelPath)
	if err != nil {
		return err
	}

	// Check if the signatures match
	expected := len(witnesses)
	match := 0
	for _, modificationSignature := range laoState.ModificationSignatures {
		err = schnorr.VerifyWithChecks(crypto.Suite, []byte(modificationSignature.Witness),
			[]byte(laoState.ModificationID), []byte(modificationSignature.Signature))
		if err != nil {
			return errors.NewInvalidMessageFieldError("failed to verify signature for witness: %s",
				modificationSignature.Witness)
		}
		if _, ok := witnesses[modificationSignature.Witness]; ok {
			match++
		}
	}

	if match != expected {
		return errors.NewInvalidMessageFieldError("not enough witness signatures provided. Needed %d got %d",
			expected, match)
	}

	var updateMsgData messagedata.LaoUpdate

	err = msg.UnmarshalData(&updateMsgData)
	if err != nil {
		return err
	}

	err = updateMsgData.Verify()
	if err != nil {
		return err
	}

	return compareLaoUpdateAndState(updateMsgData, laoState)
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) error {
	if update.LastModified != state.LastModified {
		return errors.NewInvalidMessageFieldError("mismatch between last modified: expected %d got %d",
			update.LastModified, state.LastModified)
	}

	if update.Name != state.Name {
		return errors.NewInvalidMessageFieldError("mismatch between name: expected %s got %s",
			update.Name, state.Name)
	}

	numUpdateWitnesses := len(update.Witnesses)
	numStateWitnesses := len(state.Witnesses)

	if numUpdateWitnesses != numStateWitnesses {
		return errors.NewInvalidMessageFieldError("mismatch between witness count")
	}

	match := 0
	for _, updateWitness := range update.Witnesses {
		if slices.Contains(state.Witnesses, updateWitness) {
			match++
		}
	}
	if match != numUpdateWitnesses {
		return errors.NewInvalidMessageFieldError("mismatch between witness keys")
	}

	return nil
}

// Not implemented yet
func handleLaoUpdate(msg message.Message) error {
	return nil
}

// Not implemented yet
func handleMeetingCreate(msg message.Message) error {
	return nil
}

// Not implemented yet
func handleMeetingState(msg message.Message) error {
	return nil
}

// Not implemented yet
func handleMessageWitness(msg message.Message) error { return nil }
