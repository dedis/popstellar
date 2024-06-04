package channel

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/repository"
	"popstellar/internal/validation"
	"strings"

	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
)

type chirpHandler struct {
	conf   repository.ConfigManager
	subs   repository.SubscriptionManager
	db     repository.ChirpRepository
	schema *validation.SchemaValidator
}

func createChripHandler(conf repository.ConfigManager, subs repository.SubscriptionManager,
	db repository.ChirpRepository, schema *validation.SchemaValidator) *chirpHandler {
	return &chirpHandler{
		conf:   conf,
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (c *chirpHandler) handle(channelPath string, msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = c.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		err = c.handleChirpAdd(channelPath, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
		err = c.handleChirpDelete(channelPath, msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	generalMsg, err := c.createChirpNotify(channelPath, msg)
	if err != nil {
		return err
	}

	generalChirpsChannelID, ok := strings.CutSuffix(channelPath, Social+"/"+msg.Sender)
	if !ok {
		return errors.NewInvalidMessageFieldError("invalid channelPath path %s", channelPath)
	}

	err = c.db.StoreChirpMessages(channelPath, generalChirpsChannelID, msg, generalMsg)
	if err != nil {
		return err
	}

	err = c.subs.BroadcastToAllClients(msg, channelPath)
	if err != nil {
		return err
	}

	err = c.subs.BroadcastToAllClients(generalMsg, generalChirpsChannelID)
	if err != nil {
		return err
	}

	return nil
}

func (c *chirpHandler) handleChirpAdd(channelID string, msg message.Message) error {
	var data messagedata.ChirpAdd
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return c.verifyChirpMessage(channelID, msg, data)
}

func (c *chirpHandler) handleChirpDelete(channelID string, msg message.Message) error {
	var data messagedata.ChirpDelete
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	err = c.verifyChirpMessage(channelID, msg, data)
	if err != nil {
		return err
	}

	msgToDeleteExists, err := c.db.HasMessage(data.ChirpID)
	if err != nil {
		return err
	}
	if !msgToDeleteExists {
		return errors.NewInvalidResourceError("cannot delete unknown chirp")
	}

	return nil
}

func (c *chirpHandler) verifyChirpMessage(channelID string, msg message.Message, chirpMsg messagedata.Verifiable) error {
	err := chirpMsg.Verify()
	if err != nil {
		return err
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		return errors.NewAccessDeniedError("only the owner of the channelPath can post chirps")
	}

	return nil
}

func (c *chirpHandler) createChirpNotify(channelID string, msg message.Message) (message.Message, error) {
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

	pkBuf, err := c.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, err := c.conf.Sign(dataBuf)
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
