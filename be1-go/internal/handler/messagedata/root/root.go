package root

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	messageHandler "popstellar/internal/handler/message"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/validation"
)

const (
	Root       = "/root"
	RootPrefix = "/root/"
	Social     = "/social"
	Chirps     = "/chirps"
	Reactions  = "/reactions"
	Consensus  = "/consensus"
	Coin       = "/coin"
	Auth       = "/authentication"
	Federation = "/federation"
)

type Config interface {
	GetOwnerPublicKey() kyber.Point
	GetServerPublicKey() kyber.Point
	GetServerInfo() (string, string, string, error)
	Sign(data []byte) ([]byte, error)
}

type Subscribers interface {
	AddChannel(channel string) error
}

type Peers interface {
	GetAllPeersInfo() []method.GreetServerParams
}

type Repository interface {

	// StoreLaoWithLaoGreet stores a list of "sub" channels, a message and a lao greet message inside the database.
	StoreLaoWithLaoGreet(
		channels map[string]string,
		laoID string,
		organizerPubBuf []byte,
		msg, laoGreetMsg message.Message) error

	// StoreMessageAndData stores a message inside the database.
	StoreMessageAndData(channelID string, msg message.Message) error

	// HasChannel returns true if the channel already exists.
	HasChannel(channel string) (bool, error)
}

type Handler struct {
	conf   Config
	subs   Subscribers
	peers  Peers
	db     Repository
	schema *validation.SchemaValidator
}

func New(config Config, db Repository,
	subs Subscribers, peers Peers, schema *validation.SchemaValidator) *Handler {
	return &Handler{
		conf:   config,
		subs:   subs,
		peers:  peers,
		db:     db,
		schema: schema,
	}
}

func (h *Handler) Handle(_ string, msg message.Message) error {

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewDecodeStringError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionCreate:
		err = h.handleLaoCreate(msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	return err
}

func (h *Handler) handleLaoCreate(msg message.Message) error {

	var laoCreate messagedata.LaoCreate
	err := msg.UnmarshalData(&laoCreate)
	if err != nil {
		return err
	}

	laoPath := RootPrefix + laoCreate.ID

	organizerPubBuf, err := h.verifyLaoCreation(msg, laoCreate, laoPath)
	if err != nil {
		return err
	}

	laoGreetMsg, err := h.createLaoGreet(organizerPubBuf, laoCreate.ID)
	if err != nil {
		return err
	}

	return h.createLaoAndChannels(msg, laoGreetMsg, organizerPubBuf, laoPath)
}

func (h *Handler) verifyLaoCreation(msg message.Message, laoCreate messagedata.LaoCreate, laoPath string) ([]byte, error) {

	ok, err := h.db.HasChannel(laoPath)
	if err != nil {
		return nil, err
	} else if ok {
		return nil, errors.NewDuplicateResourceError("duplicate lao path: %s", laoPath)
	}

	err = laoCreate.Verify()
	if err != nil {
		return nil, err
	}

	senderPubBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderPubBuf)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
	}

	organizerPubBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
	}
	// Check if the sender and organizer fields of the create#lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		return nil, errors.NewAccessDeniedError("sender's public key does not match the organizer public key: %s != %s",
			senderPubKey, organizerPubKey)
	}

	ownerPublicKey := h.conf.GetOwnerPublicKey()

	// Check if the sender of the LAO creation message is the owner
	if ownerPublicKey != nil && !ownerPublicKey.Equal(senderPubKey) {
		return nil, errors.NewAccessDeniedError("sender's public key does not match the owner public key: %s != %s",
			senderPubKey, ownerPublicKey)
	}

	return organizerPubBuf, nil
}

func (h *Handler) createLaoAndChannels(msg, laoGreetMsg message.Message, organizerPubBuf []byte, laoPath string) error {
	channels := map[string]string{
		laoPath:                      messageHandler.LaoType,
		laoPath + Social + Chirps:    messageHandler.ChirpType,
		laoPath + Social + Reactions: messageHandler.ReactionType,
		laoPath + Consensus:          messageHandler.ConsensusType,
		laoPath + Coin:               messageHandler.CoinType,
		laoPath + Auth:               messageHandler.AuthType,
		laoPath + Federation:         messageHandler.FederationType,
	}

	err := h.db.StoreLaoWithLaoGreet(channels, laoPath, organizerPubBuf, msg, laoGreetMsg)
	if err != nil {
		return err
	}

	for channelPath := range channels {
		err = h.subs.AddChannel(channelPath)
		if err != nil {
			return err
		}
	}

	return nil
}

func (h *Handler) createLaoGreet(organizerBuf []byte, laoID string) (message.Message, error) {
	peersInfo := h.peers.GetAllPeersInfo()

	knownPeers := make([]messagedata.Peer, 0, len(peersInfo))
	for _, info := range peersInfo {
		knownPeers = append(knownPeers, messagedata.Peer{Address: info.ClientAddress})
	}

	_, clientServerAddress, _, err := h.conf.GetServerInfo()
	if err != nil {
		return message.Message{}, err
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoID,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  clientServerAddress,
		Peers:    knownPeers,
	}

	// Marshall the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey := h.conf.GetServerPublicKey()

	// Marshall the server public key
	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to marshal server public key: %v", err)
	}

	// sign the data
	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return message.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         message.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return laoGreetMsg, nil
}
