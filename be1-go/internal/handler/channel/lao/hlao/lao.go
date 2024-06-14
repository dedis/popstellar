package hlao

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/election/melection"
	"popstellar/internal/handler/channel/lao/mlao"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/validation"
	"strings"
)

type Config interface {
	GetServerPublicKey() kyber.Point
	Sign(data []byte) ([]byte, error)
}

type Subscribers interface {
	HasChannel(channel string) bool
	BroadcastToAllClients(msg mmessage.Message, channel string) error
	AddChannel(channel string) error
}

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
	StoreRollCallClose(channels []string, laoID string, msg mmessage.Message) error

	// StoreElectionWithElectionKey stores an electionSetup message and an election key message inside the database.
	StoreElectionWithElectionKey(
		laoPath, electionPath string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg, electionKeyMsg mmessage.Message) error

	//StoreElection stores an electionSetup message inside the database.
	StoreElection(
		laoPath, electionPath string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg mmessage.Message) error

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)
}

type Handler struct {
	conf   Config
	subs   Subscribers
	db     Repository
	schema *validation.SchemaValidator
	log    zerolog.Logger
}

func New(conf Config, subs Subscribers, db Repository, schema *validation.SchemaValidator, log zerolog.Logger) *Handler {
	return &Handler{
		conf:   conf,
		subs:   subs,
		db:     db,
		schema: schema,
		log:    log.With().Str("module", "lao").Logger(),
	}
}

func (h *Handler) Handle(channelPath string, msg mmessage.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	storeMessage := true
	switch object + "#" + action {
	case channel.RollCallObject + "#" + channel.RollCallActionClose:
		storeMessage = false
		err = h.handleRollCallClose(msg, channelPath)
	case channel.RollCallObject + "#" + channel.RollCallActionCreate:
		err = h.handleRollCallCreate(msg, channelPath)
	case channel.RollCallObject + "#" + channel.RollCallActionOpen:
		err = h.handleRollCallOpen(msg, channelPath)
	case channel.RollCallObject + "#" + channel.RollCallActionReOpen:
		err = h.handleRollCallReOpen(msg, channelPath)
	case channel.ElectionObject + "#" + channel.ElectionActionSetup:
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

func (h *Handler) handleRollCallCreate(msg mmessage.Message, channelPath string) error {
	var rollCallCreate mlao.RollCallCreate
	err := msg.UnmarshalData(&rollCallCreate)
	if err != nil {
		return err
	}

	return rollCallCreate.Verify(channelPath)
}

func (h *Handler) handleRollCallOpen(msg mmessage.Message, channelPath string) error {
	var rollCallOpen mlao.RollCallOpen
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

func (h *Handler) handleRollCallReOpen(msg mmessage.Message, channelPath string) error {
	var rollCallReOpen mlao.RollCallReOpen
	err := msg.UnmarshalData(&rollCallReOpen)
	if err != nil {
		return err
	}

	return h.handleRollCallOpen(msg, channelPath)
}

func (h *Handler) handleRollCallClose(msg mmessage.Message, channelPath string) error {
	var rollCallClose mlao.RollCallClose
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

func (h *Handler) createNewAttendeeChannels(channelPath string, rollCallClose mlao.RollCallClose) ([]string, error) {
	channels := make([]string, 0, len(rollCallClose.Attendees))

	for _, popToken := range rollCallClose.Attendees {
		_, err := base64.URLEncoding.DecodeString(popToken)
		if err != nil {
			return nil, errors.NewInvalidMessageFieldError("failed to decode poptoken: %v", err)
		}

		chirpingChannelPath := channelPath + channel.Social + "/" + popToken
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

func (h *Handler) handleElectionSetup(msg mmessage.Message, channelPath string) error {
	var electionSetup mlao.ElectionSetup
	err := msg.UnmarshalData(&electionSetup)
	if err != nil {
		return err
	}

	err = h.verifySenderLao(channelPath, msg)
	if err != nil {
		return err
	}

	laoID, _ := strings.CutPrefix(channelPath, channel.RootPrefix)

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

func (h *Handler) verifySenderLao(channelPath string, msg mmessage.Message) error {
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

func (h *Handler) storeElection(msg mmessage.Message, electionSetup mlao.ElectionSetup, channelPath string) error {
	electionPubKey, electionSecretKey := h.generateKeys()
	electionPath := channelPath + "/" + electionSetup.ID

	if electionSetup.Version == mlao.SecretBallot {
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

func (h *Handler) createElectionKey(electionID string, electionPubKey kyber.Point) (mmessage.Message, error) {
	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to marshal election public key: %v", err)
	}

	msgData := melection.ElectionKey{
		Object:   channel.ElectionObject,
		Action:   channel.ElectionActionKey,
		Election: electionID,
		Key:      base64.URLEncoding.EncodeToString(electionPubBuf),
	}

	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}
	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPubBuf, err := h.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to unmarshall server secret key", err)
	}

	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return mmessage.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	electionKeyMsg := mmessage.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         channel.Hash(newData64, signature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	return electionKeyMsg, nil
}

// generateKeys generates and returns a key pair
func (*Handler) generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}
