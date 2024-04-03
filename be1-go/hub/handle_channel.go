package hub

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/validation"
)

func handleChannel(params handlerParameters, channelID string, msg message.Message) *answer.Error {
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
		errAnswer := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleChannel")
		return errAnswer
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received", msg.MessageID).Wrap("handleChannel")
		return errAnswer
	}

	channelType, err := params.db.GetChannelType(channelID)
	if err != nil {
		errAnswer := answer.NewInvalidResourceError("failed to query db: %v", err).Wrap("handleChannel")
		return errAnswer
	}

	var errAnswer *answer.Error

	switch channelType {
	case channelRoot:
		errAnswer = handleChannelRoot(params, channelID, msg)
	case channelLao:
		errAnswer = handleChannelLao(params, channelID, msg)
	case channelElection:
		errAnswer = handleChannelElection(params, channelID, msg)
	case channelGeneralChirp:
		errAnswer = handleChannelGeneralChirp(params, channelID, msg)
	case channelChirp:
		errAnswer = handleChannelChirp(params, channelID, msg)
	case channelReaction:
		errAnswer = handleChannelReaction(params, channelID, msg)
	case channelConsensus:
		errAnswer = handleChannelConsensus(params, channelID, msg)
	case channelPopCha:
		errAnswer = handleChannelPopCha(params, channelID, msg)
	case channelCoin:
		errAnswer = handleChannelCoin(params, channelID, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unknown channel type %s", channelType)
	}

	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannel")
		return errAnswer
	}

	return nil
}

// utils for the channels

func verifyDataAndGetObjectAction(params handlerParameters, msg message.Message) (object string, action string, errAnswer *answer.Error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}

	// validate message data against the json schema
	err = params.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to validate message against json schema: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}

	// get object#action
	object, action, err = messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
		errAnswer = errAnswer.Wrap("verifyDataAndGetObjectAction")
		return "", "", errAnswer
	}
	return object, action, nil
}

func Sign(data []byte, params handlerParameters) ([]byte, *answer.Error) {

	var errAnswer *answer.Error
	serverSecretBuf, err := params.db.GetServerSecretKey()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get the server secret key: %v", err)
		errAnswer = errAnswer.Wrap("Sign")
		return nil, errAnswer
	}

	serverSecretKey := crypto.Suite.Scalar()
	err = serverSecretKey.UnmarshalBinary(serverSecretBuf)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to unmarshal the server secret key: %v", err)
		errAnswer = errAnswer.Wrap("Sign")
		return nil, errAnswer
	}

	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to sign the data: %v", err)
		errAnswer = errAnswer.Wrap("Sign")
		return nil, errAnswer
	}
	return signatureBuf, nil
}

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}

func broadcastToAllClients(msg message.Message, params handlerParameters, channel string) *answer.Error {
	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			channel,
			msg,
		},
	}
	var errAnswer *answer.Error
	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal broadcast query: %v", err)
		errAnswer = errAnswer.Wrap("broadcastToAllClients")
		return errAnswer
	}

	errAnswer = params.subs.SendToAll(buf, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("broadcastToAllClients")
		return errAnswer
	}

	return nil
}