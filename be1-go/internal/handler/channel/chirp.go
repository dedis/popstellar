package channel

import (
	"encoding/base64"
	"encoding/json"
	"strings"

	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
)

func handleChannelChirp(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		err = handleChirpAdd(channelPath, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
		err = handleChirpDelete(channelPath, msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	generalMsg, err := createChirpNotify(channelPath, msg)
	if err != nil {
		return err
	}

	generalChirpsChannelID, ok := strings.CutSuffix(channelPath, Social+"/"+msg.Sender)
	if !ok {
		return errors.NewInvalidMessageFieldError("invalid channelPath path %s", channelPath)
	}

	db, err := database.GetChirpRepositoryInstance()
	if err != nil {
		return err
	}

	err = db.StoreChirpMessages(channelPath, generalChirpsChannelID, msg, generalMsg)
	if err != nil {
		return err
	}

	err = broadcastToAllClients(msg, channelPath)
	if err != nil {
		return err
	}

	err = broadcastToAllClients(generalMsg, generalChirpsChannelID)
	if err != nil {
		return err
	}

	return nil
}

func handleChirpAdd(channelID string, msg message.Message) error {
	var data messagedata.ChirpAdd
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return verifyChirpMessage(channelID, msg, data)
}

func handleChirpDelete(channelID string, msg message.Message) error {
	var data messagedata.ChirpDelete
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	err = verifyChirpMessage(channelID, msg, data)
	if err != nil {
		return err
	}

	db, err := database.GetChirpRepositoryInstance()
	if err != nil {
		return err
	}

	msgToDeleteExists, err := db.HasMessage(data.ChirpID)
	if err != nil {
		return err
	}
	if !msgToDeleteExists {
		return errors.NewInvalidResourceError("cannot delete unknown chirp")
	}

	return nil
}

func verifyChirpMessage(channelID string, msg message.Message, chirpMsg messagedata.Verifiable) error {
	err := chirpMsg.Verify()
	if err != nil {
		return err
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		return errors.NewAccessDeniedError("only the owner of the channelPath can post chirps")
	}

	return nil
}

func createChirpNotify(channelID string, msg message.Message) (message.Message, error) {
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

	serverPublicKey, err := config.GetServerPublicKeyInstance()
	if err != nil {
		return message.Message{}, err
	}

	pkBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, err := sign(dataBuf)
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
