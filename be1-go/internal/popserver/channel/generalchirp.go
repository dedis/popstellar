package channel

import (
	"bytes"
	"encoding/base64"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelGeneralChirp(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyAdd:
		errAnswer = handleChirpNotifyAdd(msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyDelete:
		errAnswer = handleChirpNotifyDelete(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	db, ok := database.GetGeneralChirpRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	err := db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	return nil
}

func handleChirpNotifyAdd(msg message.Message) *answer.Error {
	var data messagedata.ChirpNotifyAdd

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).
			Wrap("handleChirpNotifyAdd")
		return errAnswer
	}

	errAnswer := verifyNotifyChirp(msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpNotifyAdd")
		return errAnswer
	}

	return nil
}

func handleChirpNotifyDelete(msg message.Message) *answer.Error {
	var data messagedata.ChirpNotifyDelete

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).
			Wrap("handleChirpNotifyDelete")
		return errAnswer
	}

	errAnswer := verifyNotifyChirp(msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpNotifyDelete")
		return errAnswer
	}

	return nil
}

// Utils

func verifyNotifyChirp(msg message.Message, chirpMsg messagedata.Verifiable) *answer.Error {
	err := chirpMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid chirp broadcast message: %v", err)
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode sender key: %v", err)
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	serverPublicKey, ok := config.GetServerPublicKeyInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("verifyNotifyChirp")
		return errAnswer
	}

	pkBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	ok = bytes.Equal(senderBuf, pkBuf)
	if !ok {
		errAnswer := answer.NewInvalidMessageFieldError("only the server can broadcast the chirp messages")
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	return nil
}
