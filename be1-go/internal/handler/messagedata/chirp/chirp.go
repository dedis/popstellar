package chirp

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/messagedata/root"
	"popstellar/internal/validation"
	"strings"

	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
)

type Config interface {
	GetServerPublicKey() kyber.Point
	Sign(data []byte) ([]byte, error)
}

type Subscribers interface {
	BroadcastToAllClients(msg message.Message, channel string) error
}

type Repository interface {
	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// StoreChirpMessages stores a chirp message and a generalChirp broadcast inside the database.
	StoreChirpMessages(channel, generalChannel string, msg, generalMsg message.Message) error
}

type Handler struct {
	conf   Config
	subs   Subscribers
	db     Repository
	schema *validation.SchemaValidator
}

func New(conf Config, subs Subscribers,
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

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		err = h.handleChirpAdd(channelPath, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
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

	generalChirpsChannelID, ok := strings.CutSuffix(channelPath, root.Social+"/"+msg.Sender)
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

func (h *Handler) handleChirpAdd(channelID string, msg message.Message) error {
	var data messagedata.ChirpAdd
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return h.verifyChirpMessage(channelID, msg, data)
}

func (h *Handler) handleChirpDelete(channelID string, msg message.Message) error {
	var data messagedata.ChirpDelete
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	err = h.verifyChirpMessage(channelID, msg, data)
	if err != nil {
		return err
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

func (h *Handler) verifyChirpMessage(channelID string, msg message.Message, chirpMsg messagedata.Verifiable) error {
	err := chirpMsg.Verify()
	if err != nil {
		return err
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		return errors.NewAccessDeniedError("only the owner of the channelPath can post chirps")
	}

	return nil
}

func (h *Handler) createChirpNotify(channelID string, msg message.Message) (message.Message, error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return message.Message{}, errors.NewInvalidMessageFieldError("failed to decode the data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		return message.Message{}, err
	}

	timestamp, err := messagedata.GetTime(jsonData)
	if err != nil {
		return message.Message{}, err
	}

	newData := messagedata.ChirpBroadcast{
		Object:    object,
		Action:    action,
		ChirpID:   msg.MessageID,
		Channel:   channelID,
		Timestamp: timestamp,
	}

	dataBuf, err := json.Marshal(newData)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	data64 := base64.URLEncoding.EncodeToString(dataBuf)

	pkBuf, err := h.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, err := h.conf.Sign(dataBuf)
	if err != nil {
		return message.Message{}, err
	}

	signature64 := base64.URLEncoding.EncodeToString(signatureBuf)

	messageID64 := message.Hash(data64, signature64)

	newMsg := message.Message{
		Data:              data64,
		Sender:            pk64,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: make([]message.WitnessSignature, 0),
	}

	return newMsg, nil
}
