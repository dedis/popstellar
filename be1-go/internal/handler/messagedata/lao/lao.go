package lao

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata/root"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
	"strings"
)

type Repository interface {
	// GetLaoWitnesses returns the list of witnesses of a LAO.
	GetLaoWitnesses(laoID string) (map[string]struct{}, error)

	// GetOrganizerPubKey returns the organizer public key of a LAO.
	GetOrganizerPubKey(laoID string) (kyber.Point, error)

	// GetRollCallState returns the state of th lao roll call.
	GetRollCallState(channel string) (string, error)

	// CheckPrevOpenOrReopenID returns true if the previous roll call open or reopen has the same ID
	CheckPrevOpenOrReopenID(channel, nextID string) (bool, error)

	// CheckPrevCreateOrCloseID returns true if the previous roll call create or close has the same ID
	CheckPrevCreateOrCloseID(channel, nextID string) (bool, error)

	// StoreRollCallClose stores a list of chirp channels and a rollCallClose message inside the database.
	StoreRollCallClose(channels []string, laoID string, msg message.Message) error

	// StoreElectionWithElectionKey stores an electionSetup message and an election key message inside the database.
	StoreElectionWithElectionKey(
		laoPath, electionPath string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg, electionKeyMsg message.Message) error

	//StoreElection stores an electionSetup message inside the database.
	StoreElection(
		laoPath, electionPath string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg message.Message) error

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg message.Message) error

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)
}

type Handler struct {
	conf   repository.ConfigManager
	subs   repository.SubscriptionManager
	db     Repository
	schema *validation.SchemaValidator
}

func New(conf repository.ConfigManager, subs repository.SubscriptionManager,
	db Repository, schema *validation.SchemaValidator) *Handler {
	return &Handler{
		conf:   conf,
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (h *Handler) Handle(channelPath string, msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionClose:
		storeMessage = false
		err = h.handleRollCallClose(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionCreate:
		err = h.handleRollCallCreate(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionOpen:
		err = h.handleRollCallOpen(msg, channelPath)
	case messagedata.RollCallObject + "#" + messagedata.RollCallActionReOpen:
		err = h.handleRollCallReOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionSetup:
		storeMessage = false
		err = h.handleElectionSetup(msg, channelPath)
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	if storeMessage {
		err = h.db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			return err
		}
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) handleRollCallCreate(msg message.Message, channelPath string) error {
	var rollCallCreate messagedata.RollCallCreate
	err := msg.UnmarshalData(&rollCallCreate)
	if err != nil {
		return err
	}

	return rollCallCreate.Verify(channelPath)
}

func (h *Handler) handleRollCallOpen(msg message.Message, channelPath string) error {
	var rollCallOpen messagedata.RollCallOpen
	err := msg.UnmarshalData(&rollCallOpen)
	if err != nil {
		return err
	}

	err = rollCallOpen.Verify(channelPath)
	if err != nil {
		return err
	}

	ok, err := h.db.CheckPrevCreateOrCloseID(channelPath, rollCallOpen.Opens)
	if err != nil {
		return err
	} else if !ok {
		return errors.NewInvalidMessageFieldError("previous id does not exist")
	}

	return nil
}

func (h *Handler) handleRollCallReOpen(msg message.Message, channelPath string) error {
	var rollCallReOpen messagedata.RollCallReOpen
	err := msg.UnmarshalData(&rollCallReOpen)
	if err != nil {
		return err
	}

	return h.handleRollCallOpen(msg, channelPath)
}

func (h *Handler) handleRollCallClose(msg message.Message, channelPath string) error {
	var rollCallClose messagedata.RollCallClose
	err := msg.UnmarshalData(&rollCallClose)
	if err != nil {
		return err
	}

	err = rollCallClose.Verify(channelPath)
	if err != nil {
		return err
	}

	ok, err := h.db.CheckPrevOpenOrReopenID(channelPath, rollCallClose.Closes)
	if err != nil {
		return err
	} else if !ok {
		return errors.NewInvalidMessageFieldError("previous id does not exist")
	}

	newChannels, err := h.createNewAttendeeChannels(channelPath, rollCallClose)
	if err != nil {
		return err
	}

	return h.db.StoreRollCallClose(newChannels, channelPath, msg)
}

func (h *Handler) createNewAttendeeChannels(channelPath string, rollCallClose messagedata.RollCallClose) ([]string, error) {
	channels := make([]string, 0, len(rollCallClose.Attendees))

	for _, popToken := range rollCallClose.Attendees {
		_, err := base64.URLEncoding.DecodeString(popToken)
		if err != nil {
			return nil, errors.NewInvalidMessageFieldError("failed to decode poptoken: %v", err)
		}

		chirpingChannelPath := channelPath + root.Social + "/" + popToken
		channels = append(channels, chirpingChannelPath)
	}

	newChannels := make([]string, 0)
	for _, channelPath := range channels {
		alreadyExists := h.subs.HasChannel(channelPath)
		if alreadyExists {
			continue
		}

		err := h.subs.AddChannel(channelPath)
		if err != nil {
			return nil, err
		}

		newChannels = append(newChannels, channelPath)
	}

	return newChannels, nil
}

func (h *Handler) handleElectionSetup(msg message.Message, channelPath string) error {
	var electionSetup messagedata.ElectionSetup
	err := msg.UnmarshalData(&electionSetup)
	if err != nil {
		return err
	}

	err = h.verifySenderLao(channelPath, msg)
	if err != nil {
		return err
	}

	laoID, _ := strings.CutPrefix(channelPath, root.RootPrefix)

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

	return h.storeElection(msg, electionSetup, channelPath)
}

func (h *Handler) verifySenderLao(channelPath string, msg message.Message) error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode sender public key: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to unmarshal sender public key: %v", err)
	}

	organizePubKey, err := h.db.GetOrganizerPubKey(channelPath)
	if err != nil {
		return err
	}
	if !organizePubKey.Equal(senderPubKey) {
		return errors.NewAccessDeniedError("sender public key does not match organizer public key: %s != %s",
			senderPubKey, organizePubKey)
	}

	return nil
}

func (h *Handler) storeElection(msg message.Message, electionSetup messagedata.ElectionSetup, channelPath string) error {
	electionPubKey, electionSecretKey := h.generateKeys()
	electionPath := channelPath + "/" + electionSetup.ID

	if electionSetup.Version == messagedata.SecretBallot {
		electionKeyMsg, err := h.createElectionKey(electionSetup.ID, electionPubKey)
		if err != nil {
			return err
		}

		err = h.db.StoreElectionWithElectionKey(channelPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)
		if err != nil {
			return err
		}
	} else {
		err := h.db.StoreElection(channelPath, electionPath, electionPubKey, electionSecretKey, msg)
		if err != nil {
			return err
		}
	}

	return h.subs.AddChannel(electionPath)
}

func (h *Handler) createElectionKey(electionID string, electionPubKey kyber.Point) (message.Message, error) {
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

	serverPubBuf, err := h.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to unmarshall server secret key", err)
	}

	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return message.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	electionKeyMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         message.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return electionKeyMsg, nil
}

// generateKeys generates and returns a key pair
func (*Handler) generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}
