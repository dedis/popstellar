package handler

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
)

func handleChannelChirp(channelID string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		errAnswer = handleChirpAdd(channelID, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
		errAnswer = handleChirpDelete(channelID, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	generalMsg, errAnswer := createChirpNotify(channelID, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	generalChirpsChannelID, errAnswer := getGeneralChirpsChannel(channelID)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	db, errAnswer := database.GetChirpRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	err := db.StoreChirpMessages(channelID, generalChirpsChannelID, msg, generalMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, channelID)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(generalMsg, generalChirpsChannelID)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelChirp")
		return errAnswer
	}

	return nil
}

func handleChirpAdd(channelID string, msg message.Message) *answer.Error {
	var data messagedata.ChirpAdd

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleChirpAdd")
		return errAnswer
	}

	errAnswer := verifyChirpMessage(channelID, msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpAdd")
		return errAnswer
	}

	return nil
}

func handleChirpDelete(channelID string, msg message.Message) *answer.Error {
	var data messagedata.ChirpDelete

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleChirpDelete")
		return errAnswer
	}

	errAnswer := verifyChirpMessage(channelID, msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpDelete")
		return errAnswer
	}

	db, errAnswer := database.GetChirpRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpDelete")
	}

	msgToDeleteExists, err := db.HasMessage(data.ChirpID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleChirpDelete")
		return errAnswer
	}
	if !msgToDeleteExists {
		errAnswer := answer.NewInvalidResourceError("cannot delete unknown chirp").Wrap("handleChirpDelete")
		return errAnswer
	}

	return nil
}

func verifyChirpMessage(channelID string, msg message.Message, chirpMsg messagedata.Verifiable) *answer.Error {
	err := chirpMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err)
		errAnswer = errAnswer.Wrap("verifyChirpMessage")
		return errAnswer
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		errAnswer := answer.NewAccessDeniedError("only the owner of the channel can post chirps")
		errAnswer = errAnswer.Wrap("verifyChirpMessage")
		return errAnswer
	}

	return nil
}

func createChirpNotify(channelID string, msg message.Message) (message.Message, *answer.Error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode the data: %v", err)
		errAnswer = errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to read the data: %v", err)
		errAnswer = errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}

	timestamp, err := messagedata.GetTime(jsonData)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to read the data: %v", err)
		errAnswer = errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
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
		errAnswer := answer.NewInvalidMessageFieldError("failed to marshal: %v", err)
		errAnswer = errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}

	data64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, ok := config.GetServerPublicKeyInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}

	pkBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		errAnswer = errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}
	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, errAnswer := Sign(dataBuf)
	if errAnswer != nil {
		errAnswer := errAnswer.Wrap("createChirpNotify")
		return message.Message{}, errAnswer
	}
	signature64 := base64.URLEncoding.EncodeToString(signatureBuf)

	messageID64 := messagedata.Hash(data64, signature64)

	newMsg := message.Message{
		Data:              data64,
		Sender:            pk64,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: make([]message.WitnessSignature, 0),
	}

	return newMsg, nil
}

func getGeneralChirpsChannel(channelID string) (string, *answer.Error) {
	channelID, _ = strings.CutPrefix(channelID, "/")
	splitChannelID := strings.Split(channelID, "/")

	if len(splitChannelID) != 4 || splitChannelID[0] != "root" || splitChannelID[2] != "social" {
		return "", answer.NewInvalidMessageFieldError("invalid channel").Wrap("getGeneralChirpsChannel")
	}

	generalChirpsChannelID := "/root/" + splitChannelID[1] + "/social/chirps"

	return generalChirpsChannelID, nil
}
