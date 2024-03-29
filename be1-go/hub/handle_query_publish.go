package hub

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
)

func handlePublish(params handlerParameters, msg []byte) (*int, error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	data := publish.Params.Message.Data
	sender := publish.Params.Message.Sender
	signature := publish.Params.Message.Signature
	messageID := publish.Params.Message.MessageID

	err = checkMidLevel(data, sender, signature, messageID)
	if err != nil {
		return &publish.ID, err
	}

	_, err = params.db.GetMessageByID(messageID)
	if err == nil {
		return &publish.ID, xerrors.Errorf("message %s was already received", messageID)
	}

	channelType, err := params.db.GetChannelType(publish.Params.Channel)
	if err != nil {
		return &publish.ID, xerrors.Errorf("channel %s doesn't exist in the database", publish.Params.Channel)
	}

	switch channelType {
	case channelRoot:
		err = handleChannelRoot(params, publish.Params.Message)
	case channelLao:
		err = handleChannelLao(params, publish.Params.Message)
	case channelElection:
		err = handleChannelElection(params, publish.Params.Message)
	case channelGeneralChirp:
		err = handleChannelGeneralChirp(params, publish.Params.Message)
	case channelChirp:
		err = handleChannelChirp(params, publish.Params.Message)
	case channelReaction:
		err = handleChannelReaction(params, publish.Params.Message)
	case channelConsensus:
		err = handleChannelConsensus(params, publish.Params.Message)
	case channelPopCha:
		err = handleChannelPopCha(params, publish.Params.Message)
	case channelCoin:
		err = handleChannelCoin(params, publish.Params.Message)
	default:
		err = xerrors.Errorf("unknown channel type %s", channelType)
	}

	return &publish.ID, err
}

func checkMidLevel(data, sender, signature, messageID string) error {
	dataBytes, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode data string: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key string: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(signature)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode signature string: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
	}

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return answer.NewInvalidMessageFieldError("message_id is wrong: expected %q found %q",
			expectedMessageID, messageID)
	}

	return nil
}
