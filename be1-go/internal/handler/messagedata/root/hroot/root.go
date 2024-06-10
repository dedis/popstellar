package hroot

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/hmessage"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/messagedata/lao/mlao"
	"popstellar/internal/handler/messagedata/root/mroot"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
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
	GetAllPeersInfo() []mgreetserver.GreetServerParams
}

type Repository interface {

	// StoreLaoWithLaoGreet stores a list of "sub" channels, a message and a lao greet message inside the database.
	StoreLaoWithLaoGreet(
		channels map[string]string,
		laoID string,
		organizerPubBuf []byte,
		msg, laoGreetMsg mmessage.Message) error

	// StoreMessageAndData stores a message inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error

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

func (h *Handler) Handle(_ string, msg mmessage.Message) error {

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

func (h *Handler) handleLaoCreate(msg mmessage.Message) error {

	var laoCreate mroot.LaoCreate
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

func (h *Handler) verifyLaoCreation(msg mmessage.Message, laoCreate mroot.LaoCreate, laoPath string) ([]byte, error) {

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

func (h *Handler) createLaoAndChannels(msg, laoGreetMsg mmessage.Message, organizerPubBuf []byte, laoPath string) error {
	channels := map[string]string{
		laoPath:                      hmessage.LaoType,
		laoPath + Social + Chirps:    hmessage.ChirpType,
		laoPath + Social + Reactions: hmessage.ReactionType,
		laoPath + Consensus:          hmessage.ConsensusType,
		laoPath + Coin:               hmessage.CoinType,
		laoPath + Auth:               hmessage.AuthType,
		laoPath + Federation:         hmessage.FederationType,
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

func (h *Handler) createLaoGreet(organizerBuf []byte, laoID string) (mmessage.Message, error) {
	peersInfo := h.peers.GetAllPeersInfo()

	knownPeers := make([]mlao.Peer, 0, len(peersInfo))
	for _, info := range peersInfo {
		knownPeers = append(knownPeers, mlao.Peer{Address: info.ClientAddress})
	}

	_, clientServerAddress, _, err := h.conf.GetServerInfo()
	if err != nil {
		return mmessage.Message{}, err
	}

	msgData := mlao.LaoGreet{
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
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey := h.conf.GetServerPublicKey()

	// Marshall the server public key
	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to marshal server public key: %v", err)
	}

	// sign the data
	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return mmessage.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := mmessage.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	return laoGreetMsg, nil
}
