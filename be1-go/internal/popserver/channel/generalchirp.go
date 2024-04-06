package channel

import (
	"bytes"
	"encoding/base64"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelGeneralChirp(params types.HandlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyAdd:
		errAnswer = handleChirpNotifyAdd(params, msg)
	case messagedata.ChirpObject + "#" + messagedata.ChirpActionNotifyDelete:
		errAnswer = handleChirpNotifyDelete(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	err := params.DB.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, params, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelGeneralChirp")
		return errAnswer
	}

	return nil
}

func handleChirpNotifyAdd(params types.HandlerParameters, msg message.Message) *answer.Error {
	var data messagedata.ChirpNotifyAdd

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).
			Wrap("handleChirpNotifyAdd")
		return errAnswer
	}

	errAnswer := verifyNotifyChirp(params, msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpNotifyAdd")
		return errAnswer
	}

	return nil
}

func handleChirpNotifyDelete(params types.HandlerParameters, msg message.Message) *answer.Error {
	var data messagedata.ChirpNotifyDelete

	err := msg.UnmarshalData(&data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).
			Wrap("handleChirpNotifyDelete")
		return errAnswer
	}

	errAnswer := verifyNotifyChirp(params, msg, data)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChirpNotifyDelete")
		return errAnswer
	}

	return nil
}

// Utils

func verifyNotifyChirp(params types.HandlerParameters, msg message.Message, chirpMsg messagedata.Verifiable) *answer.Error {
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

	pk, err := params.DB.GetServerPubKey()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err)
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	ok := bytes.Equal(senderBuf, pk)
	if !ok {
		errAnswer := answer.NewInvalidMessageFieldError("only the server can broadcast the chirp messages")
		errAnswer = errAnswer.Wrap("verifyNotifyChirp")
		return errAnswer
	}

	return nil
}
