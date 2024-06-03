package channel

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"strings"
)

func handleChannelChirp(channelPath string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	var errAnswer *answer.Error

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionAdd:
		errAnswer = handleChirpAdd(channelPath, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionDelete:
		errAnswer = handleChirpDelete(channelPath, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	generalMsg, errAnswer := createChirpNotify(channelPath, msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	generalChirpsChannelID, ok := strings.CutSuffix(channelPath, Social+"/"+msg.Sender)
	if !ok {
		errAnswer := answer.NewInvalidMessageFieldError("invalid channelPath path %s", channelPath)
		return errAnswer.Wrap("handleChannelChirp")
	}

	db, errAnswer := database.GetChirpRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	err = db.StoreChirpMessages(channelPath, generalChirpsChannelID, msg, generalMsg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleChannelChirp")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	errAnswer = broadcastToAllClients(generalMsg, generalChirpsChannelID)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelChirp")
	}

	return nil
}

func handleChirpAdd(channelID string, msg message.Message) *answer.Error {
	var data messagedata.ChirpAdd
	errAnswer := msg.UnmarshalMsgData(&data)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpAdd")
	}

	errAnswer = verifyChirpMessage(channelID, msg, data)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpAdd")
	}

	return nil
}

func handleChirpDelete(channelID string, msg message.Message) *answer.Error {
	var data messagedata.ChirpDelete
	errAnswer := msg.UnmarshalMsgData(&data)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpDelete")
	}

	errAnswer = verifyChirpMessage(channelID, msg, data)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpDelete")
	}

	db, errAnswer := database.GetChirpRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChirpDelete")
	}

	msgToDeleteExists, err := db.HasMessage(data.ChirpID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if message exists: %v", err)
		return errAnswer.Wrap("handleChirpDelete")
	}
	if !msgToDeleteExists {
		errAnswer := answer.NewInvalidResourceError("cannot delete unknown chirp")
		return errAnswer.Wrap("handleChirpDelete")
	}

	return nil
}

func verifyChirpMessage(channelID string, msg message.Message, chirpMsg messagedata.Verifiable) *answer.Error {
	err := chirpMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err)
		return errAnswer.Wrap("verifyChirpMessage")
	}

	if !strings.HasSuffix(channelID, msg.Sender) {
		errAnswer := answer.NewAccessDeniedError("only the owner of the channelPath can post chirps")
		return errAnswer.Wrap("verifyChirpMessage")
	}

	return nil
}

func createChirpNotify(channelID string, msg message.Message) (message.Message, *answer.Error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode the data: %v", err)
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to read the data: %v", err)
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
	}

	timestamp, err := messagedata.GetTime(jsonData)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to read the data: %v", err)
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
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
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
	}

	data64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, err := config.GetServerPublicKeyInstance()
	if err != nil {
		return message.Message{}, answer.NewInternalServerError(err.Error())
	}

	pkBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
	}
	pk64 := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, errAnswer := Sign(dataBuf)
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createChirpNotify")
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
