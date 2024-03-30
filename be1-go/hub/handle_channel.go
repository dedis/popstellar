package hub

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannel(params handlerParameters, channel, channelType string, msg message.Message) *answer.Error {
	dataBytes, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode data: %v", err).Wrap("handleChannel")
		return errAnswer
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key: %v", err).Wrap("handleChannel")
		return errAnswer
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode signature: %v", err).Wrap("handleChannel")
		return errAnswer
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify signature : %v", err).Wrap("handleChannel")
		return errAnswer
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		errAnswer := answer.NewInvalidActionError("messageID is wrong: expected %q found %q",
			expectedMessageID, msg.MessageID).Wrap("handleChannel")
		return errAnswer
	}

	msgAlreadyExists, err := params.db.HasMessage(msg.MessageID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("error while querying db: %v", err).Wrap("handleChannel")
		return errAnswer
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received", msg.MessageID).Wrap("handleChannel")
		return errAnswer
	}

	var errAnswer *answer.Error

	switch channelType {
	case channelRoot:
		errAnswer = handleChannelRoot(params, channel, msg)
	case channelLao:
		errAnswer = handleChannelLao(params, channel, msg)
	case channelElection:
		errAnswer = handleChannelElection(params, channel, msg)
	case channelGeneralChirp:
		errAnswer = handleChannelGeneralChirp(params, channel, msg)
	case channelChirp:
		errAnswer = handleChannelChirp(params, channel, msg)
	case channelReaction:
		errAnswer = handleChannelReaction(params, channel, msg)
	case channelConsensus:
		errAnswer = handleChannelConsensus(params, channel, msg)
	case channelPopCha:
		errAnswer = handleChannelPopCha(params, channel, msg)
	case channelCoin:
		errAnswer = handleChannelCoin(params, channel, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unknown channel type %s", channelType)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannel")
		return errAnswer
	}

	return nil
}
