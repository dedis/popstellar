package hub

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannel(params handlerParameters, channelType string, msg message.Message) error {
	dataBytes, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode data string: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key string: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode signature string: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		return answer.NewInvalidMessageFieldError("message_id is wrong: expected %q found %q",
			expectedMessageID, msg.MessageID)
	}

	_, err = params.db.GetMessageByID(msg.MessageID)
	if err == nil {
		return xerrors.Errorf("message %s was already received", msg.MessageID)
	}

	switch channelType {
	case channelRoot:
		err = handleChannelRoot(params, msg)
	case channelLao:
		err = handleChannelLao(params, msg)
	case channelElection:
		err = handleChannelElection(params, msg)
	case channelGeneralChirp:
		err = handleChannelGeneralChirp(params, msg)
	case channelChirp:
		err = handleChannelChirp(params, msg)
	case channelReaction:
		err = handleChannelReaction(params, msg)
	case channelConsensus:
		err = handleChannelConsensus(params, msg)
	case channelPopCha:
		err = handleChannelPopCha(params, msg)
	case channelCoin:
		err = handleChannelCoin(params, msg)
	default:
		err = xerrors.Errorf("unknown channel type %s", channelType)
	}

	return err
}
