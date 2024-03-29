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
		return answer.NewInvalidMessageFieldError("handleChannel: failed to decode data string: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("handleChannel: failed to decode public key string: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		return answer.NewInvalidMessageFieldError("handleChannel: failed to decode signature string: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return answer.NewInvalidMessageFieldError("handleChannel: failed to verify signature : %v", err)
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		return answer.NewInvalidActionError("handleChannel: message_id is wrong: expected %q found %q",
			expectedMessageID, msg.MessageID)
	}

	_, err = params.db.GetMessageByID(msg.MessageID)
	if err == nil {
		return answer.NewInvalidActionError("handleChannel: message %s was already received", msg.MessageID)
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
		return errAnswer.Wrap("handleChannel")
	}

	return nil
}
