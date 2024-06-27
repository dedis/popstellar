package hchirp

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/chirp/mchirp"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/validation"
	"strings"
)

type Config interface {
	GetServerPublicKey() kyber.Point
	Sign(data []byte) ([]byte, error)
}

type Subscribers interface {
	BroadcastToAllClients(msg mmessage.Message, channel string) error
}

type Repository interface {
	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// StoreChirpMessages stores a chirp message and a generalChirp broadcast inside the database.
	StoreChirpMessages(channel, generalChannel string, msg, generalMsg mmessage.Message) error
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
		log:    log.With().Str("module", "chirp").Logger(),
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

	switch object + "#" + action {
	case channel.ChirpObject + "#" + channel.ChirpActionAdd:
		err = h.handleChirpAdd(channelPath, msg)
	case channel.ChirpObject + "#" + channel.ChirpActionDelete:
		err = h.handleChirpDelete(channelPath, msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	generalMsg, err := h.createChirpNotify(channelPath, msg)
	if err != nil {
		return err
	}

	generalChirpsChannelID, ok := strings.CutSuffix(channelPath, channel.Social+"/"+msg.Sender)
	if !ok {
		return errors.NewInvalidMessageFieldError("invalid channelPath path %s", channelPath)
	}

	err = h.db.StoreChirpMessages(channelPath, generalChirpsChannelID, msg, generalMsg)
	if err != nil {
		return err
	}

	err = h.subs.BroadcastToAllClients(msg, channelPath)
	if err != nil {
		return err
	}

	err = h.subs.BroadcastToAllClients(generalMsg, generalChirpsChannelID)
	if err != nil {
		return err
	}

	return nil
}

func (h *Handler) handleChirpAdd(channelID string, msg mmessage.Message) error {
	var data mchirp.ChirpAdd
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	err = data.Verify()
	if err != nil {
		return err
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		return errors.NewAccessDeniedError("only the owner of the channelPath can post chirps")
	}

	return nil
}

func (h *Handler) handleChirpDelete(channelID string, msg mmessage.Message) error {
	var data mchirp.ChirpDelete
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	err = data.Verify()
	if err != nil {
		return err
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		return errors.NewAccessDeniedError("only the owner of the channelPath can post chirps")
	}

	msgToDeleteExists, err := h.db.HasMessage(data.ChirpID)
	if err != nil {
		return err
	}
	if !msgToDeleteExists {
		return errors.NewInvalidResourceError("cannot delete unknown chirp")
	}

	return nil
}

func (h *Handler) createChirpNotify(channelID string, msg mmessage.Message) (mmessage.Message, error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return mmessage.Message{}, errors.NewInvalidMessageFieldError("failed to decode the data: %v", err)
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		return mmessage.Message{}, err
	}

	timestamp, err := channel.GetTime(jsonData)
	if err != nil {
		return mmessage.Message{}, err
	}

	newData := mchirp.ChirpBroadcast{
		Object:    object,
		Action:    action,
		ChirpID:   msg.MessageID,
		Channel:   channelID,
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(newData)
	if err != nil {
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	data64 := base64.URLEncoding.EncodeToString(dataBuf)

	pkBuf, err := h.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return mmessage.Message{}, err
	}

	signature64 := base64.URLEncoding.EncodeToString(signatureBuf)

	messageID64 := channel.Hash(data64, signature64)

	newMsg := mmessage.Message{
		Data:              data64,
		Sender:            pk64,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: make([]mmessage.WitnessSignature, 0),
	}

	return newMsg, nil
}
