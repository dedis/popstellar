package hub

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannel(params handlerParameters, channelType string, msg message.Message) *answer.Error {
	dataBytes, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode data string: %v",
			err).Wrap("handleChannel")
		return errAnswer
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key string: %v",
			err).Wrap("handleChannel")
		return errAnswer
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode signature string: %v",
			err).Wrap("handleChannel")
		return errAnswer
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify signature : %v",
			err).Wrap("handleChannel")
		return errAnswer
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		errAnswer := answer.NewInvalidActionError("message_id is wrong: expected %q found %q",
			expectedMessageID, msg.MessageID).Wrap("handleChannel")
		return errAnswer
	}

	msgAlreadyExists, err := params.db.HasMessage(msg.MessageID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("error while querying db: %v", err).Wrap("handleChannel")
		return errAnswer
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received",
			msg.MessageID).Wrap("handleChannel")
		return errAnswer
	}

	var errAnswer *answer.Error

	switch channelType {
	case channelRoot:
		errAnswer = handleChannelRoot(params, msg)
	case channelLao:
		errAnswer = handleChannelLao(params, msg)
	case channelElection:
		errAnswer = handleChannelElection(params, msg)
	case channelGeneralChirp:
		errAnswer = handleChannelGeneralChirp(params, msg)
	case channelChirp:
		errAnswer = handleChannelChirp(params, msg)
	case channelReaction:
		errAnswer = handleChannelReaction(params, msg)
	case channelConsensus:
		errAnswer = handleChannelConsensus(params, msg)
	case channelPopCha:
		errAnswer = handleChannelPopCha(params, msg)
	case channelCoin:
		errAnswer = handleChannelCoin(params, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unknown channel type %s", channelType)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannel")
		return errAnswer
	}

	return nil
}
